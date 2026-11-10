package vn.vinabacus.xuly.phanhe;

import vn.vinabacus.doituong.phanhe.MoTaManHinh;
import java.util.*;

/** Kho thao tác DEMO trong RAM, chưa kết nối VINABACUS SQL Server. */
public final class KhoDuLieuManHinh {
    private static final Map<String,List<LinkedHashMap<String,String>>> DU_LIEU=new LinkedHashMap<>();
    private static final Map<String,String> DIEM_DANH=new LinkedHashMap<>();
    private KhoDuLieuManHinh(){}
    public static synchronized List<LinkedHashMap<String,String>> lay(MoTaManHinh.Trang trang){
        return DU_LIEU.computeIfAbsent(trang.khoa(),k->{
            List<LinkedHashMap<String,String>> rows=new ArrayList<>();
            for(String[] d:trang.mau()){
                LinkedHashMap<String,String> row=new LinkedHashMap<>();
                for(int i=0;i<trang.cot().length;i++)row.put(trang.cot()[i],i<d.length?d[i]:"");
                rows.add(row);
            }
            return rows;
        });
    }
    public static synchronized void them(MoTaManHinh.Trang trang,Map<String,String> giaTri){
        LinkedHashMap<String,String> row=new LinkedHashMap<>();
        List<LinkedHashMap<String,String>> rows=lay(trang);
        for(String key:trang.cot())row.put(key,giaTri.getOrDefault(key,""));
        for(Map.Entry<String,String> e:giaTri.entrySet())if(!row.containsKey(e.getKey()))row.put(e.getKey(),e.getValue());
        if(trang.cot().length>0) {
            String id=trang.cot()[0];
            if(row.getOrDefault(id,"").isBlank())row.put(id, taoMa(rows,trang));
        }
        rows.add(row);
    }
    private static String taoMa(List<LinkedHashMap<String,String>> rows, MoTaManHinh.Trang trang){
        String x=trang.tab().replaceAll("[^A-Za-z]","").toUpperCase(Locale.ROOT);
        return (x.length()>2?x.substring(0,2):"VB")+String.format("%04d",rows.size()+1);
    }
    public static synchronized void sua(MoTaManHinh.Trang trang,int pos,Map<String,String> giaTri){
        LinkedHashMap<String,String> row=lay(trang).get(pos);
        row.putAll(giaTri);
    }
    public static synchronized void xoa(MoTaManHinh.Trang trang,int pos){lay(trang).remove(pos);}
    public static synchronized void diemDanh(String key,String value){DIEM_DANH.put(key,value);}
    public static synchronized String layDiemDanh(String key){return DIEM_DANH.get(key);}
    public static synchronized int tongDiemDanh(){return DIEM_DANH.size();}
}
