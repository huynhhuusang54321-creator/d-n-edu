package vn.vinabacus.data;

import vn.vinabacus.model.DuLieuMau;

/** Lịch tuần minh họa; các lớp đồng thời chưa lấy từ SQL Server. */
public final class DuLieuMauLichTuan {
    private DuLieuMauLichTuan() {}
    public static DuLieuMau.Bang lay() {
        String[] cols={"Khung giờ","Thứ 2","Thứ 3","Thứ 4","Thứ 5","Thứ 6","Thứ 7"};
        Object[][] rows={
          {"08:00–09:30","Tiếng Anh A1\nKid 1","Tiếng Anh A2","Kid 1\nAbacus","Tiếng Anh A1","Tiếng Anh A2","Kid 2"},
          {"10:00–11:30","Abacus 1","Kid 2\nTiếng Anh B1","Tiếng Anh A1","Kid 1","Abacus 2","Kid 2"},
          {"14:00–15:30","Kid 1","Tiếng Anh A1","Kid 1\nAbacus","Tiếng Anh A2","Kid 2","Tiếng Anh B1"},
          {"16:00–17:30","Tiếng Anh A1\nKid 2","Toán tư duy K1","Tiếng Anh A2\nAbacus","Toán tư duy K2","Tiếng Anh B1","Kid 1"},
          {"18:00–19:30","Tiếng Anh A2","Kid 1","Tiếng Anh B1","Kid 2","Abacus 1","Kid 2"}
        };
        return new DuLieuMau.Bang(cols,rows,"Lịch tuần mẫu – nhiều lớp trong cùng khung giờ");
    }
}
