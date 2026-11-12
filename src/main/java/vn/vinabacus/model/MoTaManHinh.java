package vn.vinabacus.model;

import vn.vinabacus.model.CoSo;

/** Mô hình đặc tả một tab; không truy cập kho dữ liệu hay giao diện Swing. */
public final class MoTaManHinh {
    private MoTaManHinh() {}
    public record Trang(CoSo coSo, String menu, String tab, String moTa,
            String[] cot, String[] truong, String[] thaoTac, String[][] mau) {
        public Trang {
            cot = cot.clone();
            truong = truong.clone();
            thaoTac = thaoTac.clone();
            mau = saoChep(mau);
        }
        @Override public String[] cot() { return cot.clone(); }
        @Override public String[] truong() { return truong.clone(); }
        @Override public String[] thaoTac() { return thaoTac.clone(); }
        @Override public String[][] mau() { return saoChep(mau); }
        private static String[][] saoChep(String[][] rows) {
            String[][] copy = new String[rows.length][];
            for (int i = 0; i < rows.length; i++) copy[i] = rows[i].clone();
            return copy;
        }
        public String khoa() { return coSo.name() + "/" + menu + "/" + tab; }
    }
}
