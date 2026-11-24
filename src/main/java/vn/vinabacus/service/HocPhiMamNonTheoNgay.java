package vn.vinabacus.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Học phí từng khoảng xếp lớp; chỉ làm tròn tổng cuối một lần tới đồng. */
public final class HocPhiMamNonTheoNgay {
    private HocPhiMamNonTheoNgay(){}
    public record Doan(BigDecimal hocPhiThang,int soNgayHocChuan){}
    public record KetQua(BigDecimal tong,List<BigDecimal> theoDoan){}

    public static KetQua tinh(int soNgayChuanThang,List<Doan> ds){
        if(soNgayChuanThang<=0||ds==null||ds.isEmpty())throw new IllegalArgumentException("Thiếu số ngày chuẩn hoặc khoảng xếp lớp.");
        BigDecimal tuSo=BigDecimal.ZERO;
        int counted=0;
        for(Doan d:ds){
            if(d==null||d.hocPhiThang()==null||d.hocPhiThang().signum()<0||d.soNgayHocChuan()<0)
                throw new IllegalArgumentException("Mức học phí hoặc số ngày học không hợp lệ.");
            counted+=d.soNgayHocChuan();
            tuSo=tuSo.add(d.hocPhiThang().multiply(BigDecimal.valueOf(d.soNgayHocChuan())));
        }
        if(counted>soNgayChuanThang)throw new IllegalArgumentException("Các khoảng xếp lớp tính trùng ngày học chuẩn.");
        BigDecimal divisor=BigDecimal.valueOf(soNgayChuanThang);
        BigDecimal tong=tuSo.divide(divisor,0,RoundingMode.HALF_UP);
        List<BigDecimal> amounts=new ArrayList<>();BigDecimal allocated=BigDecimal.ZERO;
        for(int i=0;i<ds.size();i++){
            BigDecimal value=i==ds.size()-1?tong.subtract(allocated):
                ds.get(i).hocPhiThang().multiply(BigDecimal.valueOf(ds.get(i).soNgayHocChuan()))
                    .divide(divisor,0,RoundingMode.DOWN);
            amounts.add(value);allocated=allocated.add(value);
        }
        return new KetQua(tong,List.copyOf(amounts));
    }
}
