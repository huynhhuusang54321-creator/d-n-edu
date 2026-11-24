package vn.vinabacus.tv5;

import java.math.BigDecimal;
import java.util.List;
import vn.vinabacus.model.TaiChinhDTO.BangTinhChot;
import vn.vinabacus.model.TaiChinhDTO.HocPhiTrungTam;
import vn.vinabacus.model.TaiChinhDTO.KhoanThu;
import vn.vinabacus.model.TongHopThuPhi;
import vn.vinabacus.model.TongHopThuPhi.Dong;

/** Kiểm thử ghép 3 tab, không cần kết nối SQL Server. */
public final class TaiChinhGopTabSelfTest {
    private TaiChinhGopTabSelfTest() {}
    private static BigDecimal n(String x){return new BigDecimal(x);}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void money(String expected,BigDecimal actual){
        check(n(expected).compareTo(actual)==0,"Mong đợi "+expected+", nhận "+actual);
    }
    public static void main(String[] args) {
        List<HocPhiTrungTam> hp=List.of(
            new HocPhiTrungTam(11,"Huy","Tiếng Anh","A1","A1-01",n("2000000"),n("0"),n("2000000"),n("600000"),n("1400000")),
            new HocPhiTrungTam(12,"Minh Anh","Toán","T1","T1-02",n("1500000"),n("100000"),n("1400000"),n("0"),n("1400000")),
            new HocPhiTrungTam(13,"Hoàng Nam","Tiếng Anh","A2","A2-01",n("1500000"),n("0"),n("1500000"),n("1500000"),n("0")),
            new HocPhiTrungTam(14,"Miễn phí","Toán","T2","T2-01",n("0"),n("0"),n("0"),n("0"),n("0")));
        List<KhoanThu> k=List.of(
            new KhoanThu(100,1,"Huy","HOC_PHI_KHOA",n("2000000"),n("0"),n("600000"),null,"HIEU_LUC",11,null),
            new KhoanThu(101,3,"Hoàng Nam","HOC_PHI_KHOA",n("1500000"),n("0"),n("1500000"),null,"HIEU_LUC",13,null),
            new KhoanThu(102,5,"Khóa đã hủy","HOC_PHI_KHOA",n("900000"),n("0"),n("0"),null,"HUY",99,null));
        List<Dong> rows=TongHopThuPhi.trungTam(k,hp);
        check(rows.size()==5,"Đăng ký đã có khoản thu không được nhân đôi");
        check(rows.stream().filter(r->r.maNguon()==11).count()==1,"Nguồn #11 bị nhân đôi");
        check(rows.stream().filter(r->r.maNguon()==12&&r.chuaLap()).count()==1,"Thiếu đăng ký chưa lập");
        check(rows.stream().filter(r->r.maNguon()==14&&TongHopThuPhi.MIEN_PHI.equals(r.trangThai())).count()==1,
            "Khóa 0 đồng không được tạo công nợ");
        check(rows.stream().anyMatch(r->r.maKhoan()==102&&TongHopThuPhi.DA_HUY.equals(r.trangThai())),
            "Khoản hủy không được biến mất");
        Dong huy=rows.stream().filter(r->r.maNguon()==11).findFirst().orElseThrow();
        check(huy.duocThu(),"Khoản còn nợ phải cho lập phiếu thu");
        money("1400000",huy.conNo());
        TongHopThuPhi.TongQuan total=TongHopThuPhi.tongQuan(rows);
        money("3500000",total.phaiThu());money("2100000",total.daThu());
        money("1400000",total.conNo());check(total.chuaLap()==1,"Chỉ 1 khoản dự kiến");
        KhoanThu overpaid=new KhoanThu(103,6,"Thu vượt","HOC_PHI_KHOA",
            n("100000"),n("0"),n("110000"),null,"HIEU_LUC",15,null);
        Dong overpaidRow=TongHopThuPhi.trungTam(List.of(overpaid),List.of()).get(0);
        money("-10000",overpaidRow.conNo());
        check(TongHopThuPhi.THU_VUOT.equals(overpaidRow.trangThai())&&!overpaidRow.duocThu(),
            "Thu vượt phải hiển thị nợ âm và không cho thu thêm");
        money("-10000",TongHopThuPhi.tongQuan(List.of(overpaidRow)).conNo());

        List<BangTinhChot> bang=List.of(new BangTinhChot(8,"Bé An","2026-11",n("1000000"),n("300000")),
            new BangTinhChot(9,"Bé Bình","2026-11",n("500000"),n("0")));
        List<KhoanThu> kMn=List.of(
            new KhoanThu(201,7,"Bé An","TIEN_AN",n("300000"),n("-50000"),n("100000"),null,"HIEU_LUC",null,8),
            new KhoanThu(202,8,"Bé Bình","HOC_PHI_THANG",n("500000"),n("0"),n("0"),null,"HUY",null,9));
        List<Dong> mn=TongHopThuPhi.mamNon(kMn,bang);
        check(mn.size()==3,"Mầm non: 1 học phí chưa lập, 1 tiền ăn đang thu, 1 khoản hủy");
        check(mn.stream().filter(Dong::chuaLap).count()==1,"Không tạo thêm khoản 0 đồng");
        Dong meal=mn.stream().filter(r->r.maKhoan()==201).findFirst().orElseThrow();
        money("250000",meal.phaiThu());money("150000",meal.conNo());
        check(mn.stream().filter(r->r.maKhoan()==202).allMatch(r->!r.duocThu()),
            "Khoản hủy không được lập phiếu");
        TongHopThuPhi.TongQuan totalMn=TongHopThuPhi.tongQuan(mn);
        money("250000",totalMn.phaiThu());money("100000",totalMn.daThu());
        money("150000",totalMn.conNo());check(totalMn.chuaLap()==1,"Chưa lập Mầm non sai");
        System.out.println("PASS: gop Hoc phi + Khoan phai thu + Cong no, 2 co so, khong trung nguon, tinh no, khoan huy, mien phi.");
    }
}
