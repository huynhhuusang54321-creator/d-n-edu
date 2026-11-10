package vn.vinabacus;

import vn.vinabacus.model.CoSo;
import vn.vinabacus.model.DuLieuMau;
import vn.vinabacus.xuly.XuLyDuLieuMau;

public final class KiemThuDuLieuMau {
    public static void main(String[] args) {
        assert CoSo.MAM_NON.layMenu().contains("Kho – Dụng cụ");
        assert CoSo.TRUNG_TAM.layMenu().contains("Kho – Dụng cụ");
        assert CoSo.MAM_NON.layMenu().contains("Quản lý học viên");
        assert CoSo.TRUNG_TAM.layMenu().contains("Tuyển sinh");
        assert !CoSo.MAM_NON.layMenu().contains("Học bù / Đổi buổi");
        assert CoSo.TRUNG_TAM.layMenu().contains("Học bù / Đổi buổi");
        assert XuLyDuLieuMau.layLich(CoSo.TRUNG_TAM).stream().anyMatch(x->x.noiDung().size()>=3);
        assert XuLyDuLieuMau.layLich(CoSo.MAM_NON).size()==5;
        DuLieuMau.SoLieu m=XuLyDuLieuMau.laySoLieu(CoSo.MAM_NON);
        DuLieuMau.SoLieu t=XuLyDuLieuMau.laySoLieu(CoSo.TRUNG_TAM);
        assert m.daDiemDanh()==128 && m.coMat()==120;
        assert t.daDiemDanh()==205 && t.coMat()==192;
        assert Math.abs(t.tiLe(192)-93.6585)<0.01;
        for(CoSo coSo:CoSo.values()) {
            for(String menu:coSo.layMenu()) {
                if(menu.equals("Trang chủ"))continue;
                DuLieuMau.Bang b=XuLyDuLieuMau.layBang(coSo,menu);
                assert b.cot().length>0 && b.dong().length>0 : coSo+" / "+menu;
                for(Object[] d:b.dong())assert d.length==b.cot().length;
            }
        }
        System.out.println("PASS: menu 2 cơ sở, lịch nhiều lớp, số liệu, các màn hình mẫu.");
    }
}
