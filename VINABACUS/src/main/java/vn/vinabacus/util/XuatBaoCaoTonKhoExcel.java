package vn.vinabacus.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import vn.vinabacus.model.BaoCaoTonKho;

/**
 * Xuat bao cao ton kho theo bo cuc sheet T10 do nguoi dung cung cap.
 * 10 cot: STT, Ten HH, DVT, Gia ban, Ton dau, Nhap, Xuat hoc vien,
 * Xuat co so, Ton cuoi, Ghi chu.
 * Khong can Apache POI; khong thay doi du lieu SQL Server.
 */
public final class XuatBaoCaoTonKhoExcel {

    private XuatBaoCaoTonKhoExcel() { }

    public static void xuat(Path duongDan, List<BaoCaoTonKho> duLieu,
                            String tenCoSo, int thang, int nam) throws IOException {
        if (duongDan == null || duLieu == null) {
            throw new IllegalArgumentException("Chưa có đường dẫn hoặc dữ liệu xuất.");
        }
        if (thang < 1 || thang > 12 || nam < 2000 || nam > 2100) {
            throw new IllegalArgumentException("Tháng hoặc năm không hợp lệ.");
        }
        if (duLieu.size() > 1_048_569) {
            throw new IOException("Số mặt hàng vượt giới hạn trang Excel.");
        }

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(duongDan))) {
            zip.setLevel(6);
            them(zip, "[Content_Types].xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                </Types>
                """);
            them(zip, "_rels/.rels", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """);
            String tenSheet = "T" + thang + "." + String.format("%02d", nam % 100);
            them(zip, "xl/workbook.xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                 xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                 <sheets><sheet name=""" + "\"" + tenSheet + "\"" + """ 
                    sheetId="1" r:id="rId1"/></sheets>
                 <calcPr calcId="191029" fullCalcOnLoad="1"/>
                </workbook>
                """);
            them(zip, "xl/_rels/workbook.xml.rels", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                </Relationships>
                """);
            them(zip, "xl/styles.xml", taoStyles());
            them(zip, "xl/worksheets/sheet1.xml", taoTrangTinh(duLieu, tenCoSo, thang, nam));
        }
    }

    private static String taoStyles() {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <numFmts count="3">
                <numFmt numFmtId="164" formatCode="#,##0"/>
                <numFmt numFmtId="165" formatCode="#,##0.000"/>
                <numFmt numFmtId="166" formatCode="#,##0.00"/>
              </numFmts>
              <fonts count="6">
                <font><sz val="11"/><name val="Times New Roman"/></font>
                <font><b/><sz val="12"/><name val="Times New Roman"/></font>
                <font><b/><sz val="16"/><name val="Times New Roman"/></font>
                <font><b/><i/><sz val="12"/><color rgb="FF124AC0"/><name val="Times New Roman"/></font>
                <font><b/><sz val="11"/><name val="Times New Roman"/></font>
                <font><b/><sz val="11"/><color rgb="FFF00000"/><name val="Times New Roman"/></font>
              </fonts>
              <fills count="7">
                <fill><patternFill patternType="none"/></fill>
                <fill><patternFill patternType="gray125"/></fill>
                <fill><patternFill patternType="solid"><fgColor rgb="FF92D050"/><bgColor indexed="64"/></patternFill></fill>
                <fill><patternFill patternType="solid"><fgColor rgb="FFA9DFBF"/><bgColor indexed="64"/></patternFill></fill>
                <fill><patternFill patternType="solid"><fgColor rgb="FFFFC5A0"/><bgColor indexed="64"/></patternFill></fill>
                <fill><patternFill patternType="solid"><fgColor rgb="FFB0E0E6"/><bgColor indexed="64"/></patternFill></fill>
                <fill><patternFill patternType="solid"><fgColor rgb="FF9FE0B4"/><bgColor indexed="64"/></patternFill></fill>
              </fills>
              <borders count="2">
                <border><left/><right/><top/><bottom/><diagonal/></border>
                <border><left style="thin"><color rgb="FF333333"/></left>
                  <right style="thin"><color rgb="FF333333"/></right>
                  <top style="thin"><color rgb="FF333333"/></top>
                  <bottom style="thin"><color rgb="FF333333"/></bottom><diagonal/></border>
              </borders>
              <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
              <cellXfs count="19">
                <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
                <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyAlignment="1" applyFont="1"><alignment horizontal="center" vertical="center"/></xf>
                <xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyAlignment="1" applyFont="1"><alignment horizontal="center" vertical="center"/></xf>
                <xf numFmtId="0" fontId="4" fillId="2" borderId="1" xfId="0" applyAlignment="1" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
                <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyAlignment="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyAlignment="1" applyBorder="1"><alignment horizontal="left" vertical="center" wrapText="1" indent="1"/></xf>
                <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyAlignment="1" applyNumberFormat="1" applyBorder="1"><alignment horizontal="right" vertical="center"/></xf>
                <xf numFmtId="164" fontId="5" fillId="3" borderId="1" xfId="0" applyAlignment="1" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                <xf numFmtId="164" fontId="0" fillId="4" borderId="1" xfId="0" applyAlignment="1" applyNumberFormat="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                <xf numFmtId="164" fontId="0" fillId="5" borderId="1" xfId="0" applyAlignment="1" applyNumberFormat="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                <xf numFmtId="164" fontId="5" fillId="6" borderId="1" xfId="0" applyAlignment="1" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyAlignment="1" applyBorder="1"><alignment horizontal="left" vertical="center" wrapText="1"/></xf>
                <xf numFmtId="166" fontId="0" fillId="0" borderId="1" xfId="0" applyAlignment="1" applyNumberFormat="1" applyBorder="1"><alignment horizontal="right" vertical="center"/></xf>
                <xf numFmtId="165" fontId="5" fillId="3" borderId="1" xfId="0" applyAlignment="1" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                <xf numFmtId="165" fontId="0" fillId="4" borderId="1" xfId="0" applyAlignment="1" applyNumberFormat="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                <xf numFmtId="165" fontId="0" fillId="5" borderId="1" xfId="0" applyAlignment="1" applyNumberFormat="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                <xf numFmtId="165" fontId="5" fillId="6" borderId="1" xfId="0" applyAlignment="1" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
              </cellXfs>
            </styleSheet>
            """;
    }

    private static String taoTrangTinh(List<BaoCaoTonKho> duLieu,
                                       String tenCoSo, int thang, int nam) {
        StringBuilder b = new StringBuilder(5000 + duLieu.size() * 800);
        b.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        b.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        b.append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"7\" topLeftCell=\"A8\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>");
        b.append("<sheetFormatPr defaultRowHeight=\"19\"/>");
        b.append("<cols>");
        int[] widths = {7, 44, 12, 18, 16, 16, 18, 18, 17, 37};
        for (int i = 0; i < widths.length; i++) {
            b.append("<col min=\"").append(i + 1).append("\" max=\"")
             .append(i + 1).append("\" width=\"").append(widths[i])
             .append("\" customWidth=\"1\"/>");
        }
        b.append("</cols><sheetData>");
        batDauHang(b, 1, 24); chu(b, "B1", "VINABACUS - QUẢN LÝ GIÁO DỤC", 1); ketThucHang(b);
        batDauHang(b, 2, 21); chu(b, "B2", "Cơ sở: " + (tenCoSo == null ? "" : tenCoSo), 1); ketThucHang(b);
        batDauHang(b, 3, 20); chu(b, "B3", "Báo cáo được xuất từ phần mềm VINABACUS", 2); ketThucHang(b);
        batDauHang(b, 4, 34); chu(b, "A4", "BÁO CÁO XUẤT - NHẬP - TỒN KHO NGUYÊN HỌC LIỆU", 3); ketThucHang(b);
        batDauHang(b, 5, 26); chu(b, "A5", "Tháng " + thang + " năm " + nam, 4); ketThucHang(b);

        batDauHang(b, 6, 45);
        String[] top = {"STT", "Tên Hàng Hoá", "ĐVT", "Đơn giá\nxuất BÁN",
            "Tồn Kho\nĐầu Kỳ T" + thang, "Nhập\nTrong Kỳ (T" + thang + ")",
            "Xuất cho\nHỌC VIÊN (T" + thang + ")", "Xuất cho\nCƠ SỞ (T" + thang + ")",
            "Tồn Kho\nCuối Kỳ (T" + thang + ")", "Ghi Chú"};
        for (int c = 0; c < top.length; c++) chu(b, cot(c) + "6", top[c], 5);
        ketThucHang(b);
        batDauHang(b, 7, 25);
        for (int c = 4; c <= 8; c++) {
            chu(b, cot(c) + "7", "SL", 5);
        }
        ketThucHang(b);
        int row = 8;
        for (BaoCaoTonKho d : duLieu) {
            batDauHang(b, row, 29);
            so(b, "A" + row, BigDecimal.valueOf(row - 7), 6);
            chu(b, "B" + row, d.getTenHangHoa(), 7);
            chu(b, "C" + row, d.getDonViTinh(), 6);
            if (d.getDonGiaXuatBan() != null) {
                so(b, "D" + row, d.getDonGiaXuatBan(), 8);
            } else {
                chu(b, "D" + row, "", 8); // gia ban khong co trong du lieu cu
            }
            so(b, "E" + row, d.getTonDau(), 9);
            so(b, "F" + row, d.getNhap(), 10);
            so(b, "G" + row, d.getXuatHocVien(), 11);
            so(b, "H" + row, d.getXuatCoSo(), 11);
            String formula = "E" + row + "+F" + row + "-G" + row + "-H" + row;
            BigDecimal dc = so0(d.getDieuChinh());
            if (dc.signum() != 0) formula += (dc.signum() > 0 ? "+" : "") + dc.stripTrailingZeros().toPlainString();
            congThuc(b, "I" + row, formula, d.getTonCuoi(), 12);
            String ghiChu = dc.signum() == 0 ? "" : "Điều chỉnh kiểm kê: "
                + (dc.signum() > 0 ? "+" : "") + dc.stripTrailingZeros().toPlainString();
            chu(b, "J" + row, ghiChu, 13);
            ketThucHang(b);
            row++;
        }
        b.append("</sheetData>");
        b.append("<mergeCells count=\"10\">");
        for (String m : new String[]{"B1:J1", "B2:J2", "B3:J3", "A4:J4", "A5:J5",
            "A6:A7", "B6:B7", "C6:C7", "D6:D7", "J6:J7"}) {
            b.append("<mergeCell ref=\"").append(m).append("\"/>");
        }
        b.append("</mergeCells>");
        b.append("<pageMargins left=\"0.3\" right=\"0.3\" top=\"0.5\" bottom=\"0.5\" header=\"0.2\" footer=\"0.2\"/>");
        b.append("<pageSetup orientation=\"landscape\" paperSize=\"9\" fitToWidth=\"1\" fitToHeight=\"0\"/>");
        b.append("</worksheet>");
        return b.toString();
    }

    private static BigDecimal so0(BigDecimal so) {
        return so == null ? BigDecimal.ZERO : so;
    }

    private static void batDauHang(StringBuilder b, int r, int height) {
        b.append("<row r=\"").append(r).append("\" ht=\"").append(height).append("\" customHeight=\"1\">");
    }

    private static void ketThucHang(StringBuilder b) {
        b.append("</row>");
    }

    private static String cot(int i) {
        return String.valueOf((char) ('A' + i));
    }

    private static void so(StringBuilder b, String address, BigDecimal so, int style) {
        BigDecimal value = so0(so).stripTrailingZeros();
        if (value.scale() > 0) {
            if (style == 8) style = 14;
            else if (style >= 9 && style <= 12) style = style + 6;
        }
        b.append("<c r=\"").append(address).append("\" s=\"").append(style).append("\"><v>")
         .append(value.toPlainString()).append("</v></c>");
    }

    private static void congThuc(StringBuilder b, String address, String formula,
                                  BigDecimal cached, int style) {
        if (so0(cached).stripTrailingZeros().scale() > 0 && style == 12) {
            style = 18;
        }
        b.append("<c r=\"").append(address).append("\" s=\"").append(style).append("\"><f>")
         .append(formula).append("</f><v>")
         .append(so0(cached).stripTrailingZeros().toPlainString()).append("</v></c>");
    }

    private static void chu(StringBuilder b, String address, String value, int style) {
        b.append("<c r=\"").append(address).append("\" s=\"").append(style)
         .append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">");
        String s = value == null ? "" : value;
        s.codePoints().forEach(cp -> {
            if (cp == '&') b.append("&amp;");
            else if (cp == '<') b.append("&lt;");
            else if (cp == '>') b.append("&gt;");
            else if (cp == '"') b.append("&quot;");
            else if (cp == '\'') b.append("&apos;");
            else if (cp == 9 || cp == 10 || cp == 13 || cp >= 32 && cp <= 0xD7FF
                 || cp >= 0xE000 && cp <= 0xFFFD || cp >= 0x10000 && cp <= 0x10FFFF) {
                b.appendCodePoint(cp);
            }
        });
        b.append("</t></is></c>");
    }

    private static void them(ZipOutputStream zip, String name, String xml) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(xml.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
