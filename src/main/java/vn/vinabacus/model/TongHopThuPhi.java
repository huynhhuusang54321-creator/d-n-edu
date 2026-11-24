package vn.vinabacus.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import vn.vinabacus.model.TaiChinhDTO.BangTinhChot;
import vn.vinabacus.model.TaiChinhDTO.HocPhiTrungTam;
import vn.vinabacus.model.TaiChinhDTO.KhoanThu;

/**
 * Ghép học phí/tiền ăn theo nguồn với khoản phải thu thật.
 * Một nguồn đã lập khoản thu KHÔNG được hiện thêm lần nữa như "Chưa lập thu".
 * Đây chỉ là dữ liệu trình bày, không thay đổi nghiệp vụ/SQL của các thành viên khác.
 */
public final class TongHopThuPhi {
    private TongHopThuPhi() {}
    public static final String CHUA_LAP="Chưa lập thu";
    public static final String CHUA_DONG="Chưa đóng";
    public static final String MOT_PHAN="Đóng một phần";
    public static final String DA_DONG="Đã đóng đủ";
    public static final String THU_VUOT="Thu vượt";
    public static final String DA_HUY="Đã hủy";
    public static final String MIEN_PHI="Không phát sinh";

    public record Dong(int maKhoan, int maNguon, String loai, String hocVien,
                       String noiDung, String mon, String khoa, String lop,
                       BigDecimal goc, BigDecimal giam, BigDecimal dieuChinh,
                       BigDecimal phaiThu, BigDecimal daThu, BigDecimal conNo,
                       LocalDate han, String trangThai,int maHocVien,String soDienThoai) {
        public String maHienThi() {
            if(maKhoan>0)return "KT #"+maKhoan;
            return ("HOC_PHI_KHOA".equals(loai)?"DK #":"BT #")+maNguon;
        }
        public String tenLoai() {return switch(loai) {
            case "HOC_PHI_KHOA" -> "Học phí khóa";
            case "HOC_PHI_THANG" -> "Học phí tháng";
            case "TIEN_AN" -> "Tiền ăn";
            default -> loai;
        };}
        public boolean chuaLap() {return CHUA_LAP.equals(trangThai);}
        public boolean duocDieuChinh() {return maKhoan>0&&!DA_HUY.equals(trangThai);}
        public boolean duocThu() {return duocDieuChinh()&&conNo.signum()>0;}
    }

    private static String trangThai(KhoanThu k) {
        if("HUY".equals(k.trangThai()))return DA_HUY;
        if(k.conNo().signum()<0)return THU_VUOT;
        if(k.conNo().signum()==0)return DA_DONG;
        if(k.daThu().signum()==0)return CHUA_DONG;
        return MOT_PHAN;
    }
    private static Dong tuKhoan(KhoanThu k, String noiDung, String mon, String khoa,
                                String lop, BigDecimal goc, BigDecimal giam, int maNguon) {
        return new Dong(k.maKhoan(),maNguon,k.loai(),k.hocVien(),noiDung,mon,khoa,lop,
            goc,giam,k.dieuChinh(),k.phaiThu(),k.daThu(),k.conNo(),k.han(),trangThai(k),
            k.maHocVien(),k.soDienThoai());
    }
    private static Dong chuaLap(int id,String loai,String hocVien,String noiDung,
                                String mon,String khoa,String lop,BigDecimal goc,BigDecimal giam,
                                BigDecimal due,int maHocVien,String soDienThoai) {
        return new Dong(0,id,loai,hocVien,noiDung,mon,khoa,lop,goc,giam,
            BigDecimal.ZERO,due,BigDecimal.ZERO,BigDecimal.ZERO,null,
            due.signum()>0?CHUA_LAP:MIEN_PHI,maHocVien,soDienThoai);
    }

    public static List<Dong> trungTam(List<KhoanThu> khoan,List<HocPhiTrungTam> hocPhi) {
        List<Dong> result=new ArrayList<>();Set<Integer> daGhep=new HashSet<>();
        for(HocPhiTrungTam h:hocPhi) {
            String noiDung=h.monHoc()+" / "+h.khoaHoc()+" / "+h.lopHoc();
            boolean found=false;
            for(KhoanThu k:khoan) {
                if(!"HOC_PHI_KHOA".equals(k.loai())||k.maDangKy()==null||k.maDangKy()!=h.maDangKy())continue;
                result.add(tuKhoan(k,noiDung,h.monHoc(),h.khoaHoc(),h.lopHoc(),
                    h.goc(),h.giam(),h.maDangKy()));
                daGhep.add(k.maKhoan());found=true;
            }
            if(!found)result.add(chuaLap(h.maDangKy(),"HOC_PHI_KHOA",h.hocVien(),noiDung,
                h.monHoc(),h.khoaHoc(),h.lopHoc(),h.goc(),h.giam(),h.phaiThu(),
                h.maHocVien(),h.soDienThoai()));
        }
        // Khoản thu còn hiệu lực nhưng nguồn đăng ký không còn trong danh sách vẫn phải thấy.
        for(KhoanThu k:khoan)if(!daGhep.contains(k.maKhoan()))
            result.add(tuKhoan(k,"Khoản thu từ đăng ký #"+k.maDangKy(),"","","",
                k.goc(),BigDecimal.ZERO,k.maDangKy()==null?0:k.maDangKy()));
        return result;
    }

    public static List<Dong> mamNon(List<KhoanThu> khoan,List<BangTinhChot> bangTinh) {
        List<Dong> result=new ArrayList<>();Set<Integer> daGhep=new HashSet<>();
        for(BangTinhChot b:bangTinh) {
            themDongMamNon(result,daGhep,khoan,b,"HOC_PHI_THANG",b.hocPhi(),"Học phí tháng "+b.ky());
            themDongMamNon(result,daGhep,khoan,b,"TIEN_AN",b.tienAn(),"Tiền ăn tháng "+b.ky());
        }
        for(KhoanThu k:khoan)if(!daGhep.contains(k.maKhoan()))
            result.add(tuKhoan(k,k.tenLoai()+" / bảng tính #"+k.maBangTinh(),"","","",
                k.goc(),BigDecimal.ZERO,k.maBangTinh()==null?0:k.maBangTinh()));
        return result;
    }
    private static void themDongMamNon(List<Dong> result,Set<Integer> daGhep,
                                        List<KhoanThu> khoan,BangTinhChot b,String loai,
                                        BigDecimal amount,String noiDung) {
        boolean found=false;
        for(KhoanThu k:khoan) {
            if(!loai.equals(k.loai())||k.maBangTinh()==null||k.maBangTinh()!=b.id())continue;
            result.add(tuKhoan(k,noiDung,"","",b.ky(),amount,BigDecimal.ZERO,b.id()));
            daGhep.add(k.maKhoan());found=true;
        }
        if(!found&&amount.signum()>0)result.add(chuaLap(b.id(),loai,b.hocVien(),noiDung,
            "","",b.ky(),amount,BigDecimal.ZERO,amount,0,null));
    }

    /** Các tổng này chỉ tính khoản đã lập, đang hiệu lực; không tính học phí dự kiến. */
    public record TongQuan(BigDecimal phaiThu,BigDecimal daThu,BigDecimal conNo,int chuaLap) {}
    public static TongQuan tongQuan(List<Dong> rows) {
        BigDecimal phaiThu=BigDecimal.ZERO,daThu=BigDecimal.ZERO,conNo=BigDecimal.ZERO;
        int chuaLap=0;
        for(Dong d:rows) {
            if(d.chuaLap()){chuaLap++;continue;}
            if(d.maKhoan()==0||DA_HUY.equals(d.trangThai()))continue;
            phaiThu=phaiThu.add(d.phaiThu());daThu=daThu.add(d.daThu());conNo=conNo.add(d.conNo());
        }
        return new TongQuan(phaiThu,daThu,conNo,chuaLap);
    }
}
