package vn.vinabacus.dieukhien;

import java.util.Objects;
import vn.vinabacus.giaodien.GiaoDienChinh;
import vn.vinabacus.model.CoSo;

/** Điều phối sự kiện điều hướng. Phân quyền thật sẽ bổ sung khi có đăng nhập. */
public final class DieuKhienGiaoDienChinh {
    private final GiaoDienChinh giaoDien;
    private CoSo coSo = CoSo.MAM_NON;
    private String muc = "Trang chủ";

    public DieuKhienGiaoDienChinh(GiaoDienChinh giaoDien) {
        this.giaoDien = Objects.requireNonNull(giaoDien);
    }
    public CoSo layCoSo() { return coSo; }
    public String layMuc() { return muc; }
    public void chonCoSo(CoSo moi) {
        if(moi == null || moi == coSo) return;
        coSo=moi; muc="Trang chủ";
        giaoDien.lamMoiGiaoDien();
    }
    public void moMuc(String ten) {
        if (!coSo.layMenu().contains(ten)) return;
        muc=ten;
        giaoDien.lamMoiGiaoDien();
    }
}
