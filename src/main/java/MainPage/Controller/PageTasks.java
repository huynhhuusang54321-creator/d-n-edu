package MainPage.Controller;
import MainPage.View.Workspace;
import Model.NhanSuModel;
import Service.NhanSuService;
import java.awt.Component;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.*;

/** Điều phối tác vụ nền, không có cache DB riêng. */
public final class PageTasks {
    private PageTasks(){}
    public static <T> void run(Component owner,Callable<T> work,Consumer<T> done){new SwingWorker<T,Void>(){protected T doInBackground() throws Exception{return work.call();}protected void done(){try{done.accept(get());}catch(Exception e){Throwable x=e;while(x.getCause()!=null&&x.getCause()!=x)x=x.getCause();Workspace.message(owner,"WARNING — "+Objects.toString(x.getMessage(),x.getClass().getSimpleName()));}}}.execute();}
    public static Workspace.Choice choose(Component owner,String title,List<Workspace.Choice> choices){if(choices.isEmpty()){Workspace.message(owner,"Không có dữ liệu phù hợp: "+title);return null;}return (Workspace.Choice)JOptionPane.showInputDialog(owner,title,title,JOptionPane.PLAIN_MESSAGE,null,choices.toArray(),choices.get(0));}
    /** Chỉ phục vụ môi trường phát triển, không thay thế phiên đăng nhập. */
    public static void actor(Component owner,int branch,Consumer<Integer> done){run(owner,()->new NhanSuService().selectAll(branch).stream().filter(NhanSuModel::isTrangThai).map(x->new Workspace.Choice(x.getMaNhanSu(),x.getHoTen()+" (#"+x.getMaNhanSu()+")")).toList(),choices->{Workspace.Choice c=choose(owner,"Chọn nhân sự thực hiện (chế độ phát triển, chưa xác thực)",choices);if(c!=null)done.accept(c.id());});}
    public static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    public static String text(Object o){return Objects.toString(o,"");}
    public static String jsonString(String s){StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '\\'->b.append("\\\\");case '"'->b.append("\\\"");case '\n'->b.append("\\n");case '\r'->b.append("\\r");case '\t'->b.append("\\t");default->{if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}}return b.append('"').toString();}
    // Chỉ đọc trường scalar của CauHinh hiện tại; không suy đoán khi thiếu dữ liệu.
    public static String field(String json,String key){if(json==null)return "";var m=java.util.regex.Pattern.compile("\""+java.util.regex.Pattern.quote(key)+"\"\\s*:\\s*(?:\"((?:\\\\.|[^\"\\\\])*)\"|(-?[0-9]+(?:\\.[0-9]+)?))").matcher(json);if(!m.find())return "";return m.group(1)==null?m.group(2):m.group(1).replace("\\n","\n").replace("\\r","\r").replace("\\t","\t").replace("\\\"","\"").replace("\\\\","\\");}
    public static Integer number(String json,String key){try{return Integer.valueOf(field(json,key));}catch(Exception e){return null;}}
    /** Thay các thuộc tính cấp một, giữ nguyên JSON khác trong danh mục. */
    public static String mergeFields(String json,Map<String,String> values){
        LinkedHashMap<String,String> entries=new LinkedHashMap<>();
        if(json!=null&&!json.isBlank()){
            String s=json.trim();require(s.startsWith("{")&&s.endsWith("}"),"CauHinh không phải JSON object");int start=1,depth=0;boolean quote=false,escape=false;
            for(int i=1;i<s.length();i++){char ch=s.charAt(i);if(quote){if(escape)escape=false;else if(ch=='\\')escape=true;else if(ch=='"')quote=false;continue;}if(ch=='"'){quote=true;continue;}if(ch=='{'||ch=='[')depth++;if(ch=='}'||ch==']'){if(depth>0){depth--;continue;}if(i!=s.length()-1)throw new IllegalArgumentException("JSON không hợp lệ");}if((ch==','&&depth==0)||i==s.length()-1){String part=s.substring(start,i).trim();if(!part.isEmpty()){int colon=part.indexOf(':');require(colon>0,"JSON không hợp lệ");String key=part.substring(0,colon).trim();require(key.startsWith("\"")&&key.endsWith("\""),"JSON không hợp lệ");entries.put(key,part.substring(colon+1).trim());}start=i+1;}}
        }
        values.forEach((k,v)->entries.put(jsonString(k),v));return "{"+entries.entrySet().stream().map(e->e.getKey()+":"+e.getValue()).collect(java.util.stream.Collectors.joining(","))+"}";
    }
}
