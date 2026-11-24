package Repository;
import Data.DataConnection;
import java.sql.*;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.*;

/** Shared RAM cache. One static instance per table; synchronized per table. */
public final class TableCache<T> {
    @FunctionalInterface interface Mapper<T> { T map(ResultSet rs, Map<String,String> columns) throws SQLException; }
    public record Snapshot(List<String> columns, List<List<String>> rows) {}
    private record Row<T>(T model, List<String> values) {}
    private record Schema(List<String> columns, Map<String,String> names, List<String> missing) {}
    private record Bucket<T>(Schema schema, NavigableMap<Integer,Row<T>> rows) {}
    private final String table, pk;
    private final boolean scoped;
    private final List<String> logical;
    private final Mapper<T> mapper;
    private final UnaryOperator<T> copy;
    private final Map<Integer,Bucket<T>> buckets = new HashMap<>();
    private final Map<String,String> aliases = new HashMap<>();
    private Schema schema;
    TableCache(String table,String columns,String pk,boolean scoped,Mapper<T> mapper,UnaryOperator<T> copy) {
        this.table=table;this.pk=pk;this.scoped=scoped;this.logical=Arrays.asList(columns.split(",\\s*"));this.mapper=mapper;this.copy=copy;
    }
    private static String q(String name){return "["+name.replace("]", "]]")+"]";}
    private String tableSql(){return "dbo."+q(table);}
    private static String norm(String s){return s.replace("_", "").toLowerCase(Locale.ROOT);}
    private IllegalStateException failure(SQLException e){return new IllegalStateException("Không thể đọc/đồng bộ dbo."+table+": "+e.getMessage(),e);}
    private void scope(int branch){if(scoped&&branch<=0)throw new IllegalArgumentException("MaCoSo không hợp lệ");}
    private Schema discover(Connection c)throws SQLException{
        List<String> actual=new ArrayList<>();
        try(PreparedStatement ps=c.prepareStatement("SELECT TOP (0) * FROM "+tableSql());ResultSet rs=ps.executeQuery()){
            ResultSetMetaData md=rs.getMetaData();for(int i=1;i<=md.getColumnCount();i++)actual.add(md.getColumnName(i));
        }
        Map<String,String> names=new LinkedHashMap<>();List<String> missing=new ArrayList<>();Set<String> used=new HashSet<>();
        for(String old:logical){
            String configured=System.getProperty("vinabacus.column."+table+"."+old);
            String wanted=aliases.getOrDefault(old,configured==null?old:configured.trim());
            List<String> matches=actual.stream().filter(a->a.equalsIgnoreCase(wanted)).toList();
            if(matches.isEmpty()&&!aliases.containsKey(old)&&configured==null)matches=actual.stream().filter(a->norm(a).equals(norm(wanted))).toList();
            if(matches.size()!=1||!used.add(matches.get(0).toLowerCase(Locale.ROOT))){missing.add(old);continue;}
            names.put(old,matches.get(0));
        }
        if(!names.containsKey(pk)||(scoped&&!names.containsKey("MaCoSo")))throw new SQLException("Thiếu ánh xạ khóa chính hoặc MaCoSo; từ chối đọc ngoài phạm vi cơ sở");
        return new Schema(List.copyOf(actual),Map.copyOf(names),List.copyOf(missing));
    }
    private String col(Schema s,String name)throws SQLException{String a=s.names().get(name);if(a==null)throw new SQLException("Thiếu ánh xạ cột "+table+"."+name);return q(a);}
    private void requireTyped(Schema s){if(!s.missing().isEmpty())throw new IllegalStateException("dbo."+table+" thiếu ánh xạ Model: "+s.missing()+". Dùng selectRaw hoặc mapColumn rồi reload.");}
    private void adopt(Schema s){buckets.entrySet().removeIf(e->!e.getValue().schema().equals(s));schema=s;}
    private Bucket<T> fetch(Connection c,Schema s,int branch,Integer id)throws SQLException{
        String sql="SELECT * FROM "+tableSql();List<Integer> params=new ArrayList<>();List<String> where=new ArrayList<>();
        if(scoped){where.add(col(s,"MaCoSo")+" = ?");params.add(branch);}if(id!=null){where.add(col(s,pk)+" = ?");params.add(id);}
        if(!where.isEmpty())sql+=" WHERE "+String.join(" AND ",where);sql+=" ORDER BY "+col(s,pk)+" DESC";
        NavigableMap<Integer,Row<T>> result=new TreeMap<>(Comparator.reverseOrder());
        try(PreparedStatement ps=c.prepareStatement(sql)){
            for(int i=0;i<params.size();i++)ps.setInt(i+1,params.get(i));
            try(ResultSet rs=ps.executeQuery()){
                ResultSetMetaData md=rs.getMetaData();
                if(md.getColumnCount()!=s.columns().size())throw new SQLException("Schema thay đổi trong lúc tải; hãy reload");
                for(int i=1;i<=md.getColumnCount();i++)if(!md.getColumnName(i).equals(s.columns().get(i-1)))throw new SQLException("Schema thay đổi trong lúc tải; hãy reload");
                while(rs.next()){
                    int key=rs.getInt(s.names().get(pk));if(rs.wasNull()||key<=0)throw new SQLException("Khóa chính không hợp lệ");
                    if(scoped&&rs.getInt(s.names().get("MaCoSo"))!=branch)throw new SQLException("Dữ liệu ngoài phạm vi cơ sở");
                    List<String> raw=new ArrayList<>();
                    for(int i=0;i<s.columns().size();i++){
                        String name=s.columns().get(i);boolean secret=norm(name).contains("matkhau")||norm(name).contains("password")||name.equalsIgnoreCase(s.names().getOrDefault("MatKhauBam", ""));
                        // Nếu chưa xác định được cột mật khẩu của NhanSu, che tất cả cột chưa ánh xạ.
                        secret=secret||("NhanSu".equals(table)&&s.missing().contains("MatKhauBam")&&!s.names().containsValue(name));
                        raw.add(secret?"[Ẩn]":rs.getString(i+1));
                    }
                    T model=s.missing().isEmpty()?mapper.map(rs,s.names()):null;
                    if(result.put(key,new Row<>(model,Collections.unmodifiableList(raw)))!=null)throw new SQLException("Trùng khóa chính");
                }
            }
        }
        return new Bucket<>(s,result);
    }
    private Bucket<T> load(int branch){
        scope(branch);Bucket<T> b=buckets.get(branch);if(b!=null)return b;
        try(Connection c=DataConnection.getConnection()){Schema s=discover(c);b=fetch(c,s,branch,null);adopt(s);buckets.put(branch,b);return b;}catch(SQLException e){throw failure(e);}
    }
    synchronized List<T> all(int branch){Bucket<T>b=load(branch);requireTyped(b.schema());List<T> list=new ArrayList<>();for(Row<T> row:b.rows().values())list.add(copy.apply(row.model()));return list;}
    synchronized T byId(int branch,int id){Bucket<T>b=load(branch);requireTyped(b.schema());Row<T> row=b.rows().get(id);return row==null?null:copy.apply(row.model());}
    synchronized Snapshot raw(int branch){Bucket<T>b=load(branch);List<List<String>> rows=new ArrayList<>();for(Row<T> row:b.rows().values())rows.add(row.values());return new Snapshot(b.schema().columns(),List.copyOf(rows));}
    synchronized List<T> reload(int branch){invalidate(branch);return all(branch);}
    synchronized Snapshot reloadRaw(int branch){invalidate(branch);return raw(branch);}
    synchronized void invalidate(int branch){buckets.remove(branch);schema=null;}
    synchronized void clear(){buckets.clear();schema=null;}
    synchronized boolean loaded(int branch){return buckets.containsKey(branch);}
    synchronized void mapColumn(String old,String actual){if(!logical.contains(old)||actual==null||actual.isBlank())throw new IllegalArgumentException("Ánh xạ cột không hợp lệ");aliases.put(old,actual.trim());clear();}
    /** Preserve original write predicates; replace only known column identifiers. */
    synchronized String writeSql(Connection c,String sql)throws SQLException{
        if(schema==null)adopt(discover(c));if(!schema.missing().isEmpty())throw new SQLException("Thiếu ánh xạ cột: "+schema.missing());
        Matcher m=Pattern.compile("'(?:[^']|'')*'|[A-Za-z_][A-Za-z_0-9]*").matcher(sql);StringBuffer out=new StringBuffer();
        while(m.find()){String token=m.group();String replacement=schema.names().containsKey(token)?q(schema.names().get(token)):token;m.appendReplacement(out,Matcher.quoteReplacement(replacement));}m.appendTail(out);return out.toString();
    }
    /** Fresh row for read-modify-write; never mark a partial list as complete. */
    private T fresh(Connection c,int branch,int id)throws SQLException{
        Schema s=discover(c);Bucket<T> f=fetch(c,s,branch,id);requireTyped(s);adopt(s);Bucket<T>b=buckets.get(branch);Row<T> row=f.rows().get(id);
        if(b!=null){if(row==null)b.rows().remove(id);else b.rows().put(id,row);}return row==null?null:copy.apply(row.model());
    }
    synchronized T fresh(int branch,int id){
        scope(branch);try(Connection c=DataConnection.getConnection()) {return fresh(c,branch,id);}
        catch(SQLException|RuntimeException e){invalidate(branch);if(e instanceof SQLException sql)throw failure(sql);throw (RuntimeException)e;}
    }
    synchronized void afterWrite(Connection c,int branch,int id){
        if(!loaded(branch))return;
        try{fresh(c,branch,id);}catch(SQLException|RuntimeException e){invalidate(branch);System.err.println("["+table+"] Đã ghi DB, cache sẽ tải lại: "+e.getMessage());}
    }
    synchronized void deleted(int branch,int id){Bucket<T>b=buckets.get(branch);if(b!=null)b.rows().remove(id);}
}
