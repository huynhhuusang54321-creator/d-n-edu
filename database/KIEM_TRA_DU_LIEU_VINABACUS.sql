/* KIEM TRA DU LIEU MAU VINABACUS — Chay sau file 01 hoac 02.
   Dung SELECT, KHONG thay doi du lieu. SQL Server 2017+.
   CÁC bang nang cao (bang quyen, phien khoi phuc, bu tru tin dung, so lieu chot)
   co the trong vi chua trien khai dang nhap hoac nghiep vu chot/hoan tac.
*/
USE [VINABACUS];
GO
SET NOCOUNT ON;
IF OBJECT_ID(N'dbo.HocVien',N'U') IS NULL
   THROW 57101,N'Chua cai day du database VINABACUS.',1;
PRINT N'===== 1. TONG SO BANG, KHOA NGOAI =====';
SELECT DB_NAME() AS DatabaseName,
       (SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID(N'dbo')) AS SoBang,
       (SELECT COUNT(*) FROM sys.foreign_keys WHERE parent_object_id IN
          (SELECT object_id FROM sys.tables WHERE schema_id=SCHEMA_ID(N'dbo'))) AS SoKhoaNgoai;
IF (SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID(N'dbo'))<>57
    THROW 57102,N'So bang khac 57.',1;
IF EXISTS(SELECT 1 FROM sys.foreign_keys WHERE is_disabled=1 OR is_not_trusted=1)
    THROW 57103,N'Co khoa ngoai bi tat hoac chua duoc CHECK.',1;
PRINT N'===== 2. HOC VIEN VA LOP THEO CO SO =====';
SELECT cs.LoaiCoSo,cs.TenCoSo,
       (SELECT COUNT(*) FROM dbo.HocVien h WHERE h.MaCoSo=cs.MaCoSo) AS HocVien,
       (SELECT COUNT(*) FROM dbo.NhanVienCoSo nv WHERE nv.MaCoSo=cs.MaCoSo) AS NhanVien,
       (SELECT COUNT(*) FROM dbo.KhachTiemNang k WHERE k.MaCoSo=cs.MaCoSo) AS KhachTiemNang,
       (SELECT COUNT(*) FROM dbo.HangHoa hh WHERE hh.MaCoSo=cs.MaCoSo) AS HangHoa,
       (SELECT COUNT(*) FROM dbo.PhieuThu p WHERE p.MaCoSo=cs.MaCoSo) AS PhieuThu
FROM dbo.CoSo cs ORDER BY cs.LoaiCoSo;
PRINT N'===== 3. NGHIEP VU TRUNG TAM =====';
SELECT cs.LoaiCoSo,COUNT(DISTINCT l.MaLop) AS LopTrungTam,
       COUNT(DISTINCT dk.MaDangKy) AS DangKyKhoaHoc,
       COUNT(DISTINCT x.MaXepLop) AS XepLopTrungTam,
       COUNT(DISTINCT b.MaBuoiHoc) AS BuoiHoc
FROM dbo.CoSo cs LEFT JOIN dbo.LopTrungTam l ON l.MaCoSo=cs.MaCoSo
LEFT JOIN dbo.DangKyKhoaHoc dk ON dk.MaCoSo=cs.MaCoSo
LEFT JOIN dbo.XepLopTrungTam x ON x.MaCoSo=cs.MaCoSo
LEFT JOIN dbo.BuoiHoc b ON b.MaLop=l.MaLop
WHERE cs.LoaiCoSo='TRUNG_TAM'
GROUP BY cs.LoaiCoSo;
PRINT N'===== 4. NGHIEP VU MAM NON =====';
SELECT cs.LoaiCoSo,COUNT(DISTINCT l.MaLop) AS LopMamNon,
       COUNT(DISTINCT x.MaXepLop) AS XepLopMamNon,
       COUNT(DISTINCT dd.MaDiemDanh) AS DiemDanhMamNon,
       COUNT(DISTINCT bt.MaBangTinh) AS BangTinhPhiMamNon
FROM dbo.CoSo cs LEFT JOIN dbo.LopMamNon l ON l.MaCoSo=cs.MaCoSo
LEFT JOIN dbo.XepLopMamNon x ON x.MaCoSo=cs.MaCoSo
LEFT JOIN dbo.DiemDanhMamNon dd ON dd.MaCoSo=cs.MaCoSo
LEFT JOIN dbo.BangTinhPhiMamNon bt ON bt.MaCoSo=cs.MaCoSo
WHERE cs.LoaiCoSo='MAM_NON'
GROUP BY cs.LoaiCoSo;
PRINT N'===== 5. CHI TIET PHIEU THU DEMO CO KHOP KHONG? =====';
SELECT p.SoPhieuThu,p.TongTien,COALESCE(SUM(c.SoTienPhanBo),0) AS TongChiTiet,
       CASE WHEN p.TongTien=COALESCE(SUM(c.SoTienPhanBo),0) THEN 'PASS' ELSE 'FAIL' END AS KiemTra
FROM dbo.PhieuThu p
LEFT JOIN dbo.ChiTietPhieuThu c ON c.MaPhieuThu=p.MaPhieuThu AND c.LoaiBienDong='PHAN_BO'
WHERE p.SoPhieuThu LIKE 'DEMO-THU-%'
GROUP BY p.SoPhieuThu,p.TongTien;
PRINT N'===== 6. BANG TINH PHI MAM NON: DOI CHIEU CHI TIET =====';
SELECT b.MaBangTinh,h.HoTen,b.TongHocPhi,
       (SELECT COALESCE(SUM(ThanhTien),0) FROM dbo.ChiTietHocPhiMamNon t WHERE t.MaBangTinh=b.MaBangTinh) AS TongHocPhiChiTiet,
       b.TongTienAn,
       (SELECT COALESCE(SUM(ThanhTien),0) FROM dbo.ChiTietTienAnMamNon t WHERE t.MaBangTinh=b.MaBangTinh) AS TongTienAnChiTiet
FROM dbo.BangTinhPhiMamNon b JOIN dbo.HocVien h ON h.MaHocVien=b.MaHocVien
JOIN dbo.ThangMamNon t ON t.MaThang=b.MaThang AND t.Nam=2026 AND t.Thang=11;
PRINT N'===== 7. PHIEU KHO VA KIEM KE =====';
SELECT cs.LoaiCoSo,p.LoaiPhieu,COUNT(*) AS SoPhieu
FROM dbo.PhieuKho p JOIN dbo.CoSo cs ON cs.MaCoSo=p.MaCoSo
GROUP BY cs.LoaiCoSo,p.LoaiPhieu ORDER BY cs.LoaiCoSo,p.LoaiPhieu;
SELECT cs.LoaiCoSo,COUNT(*) AS SoKiemKe
FROM dbo.KiemKeKho kk JOIN dbo.CoSo cs ON cs.MaCoSo=kk.MaCoSo
GROUP BY cs.LoaiCoSo;
PRINT N'===== 8. SO BAN GHI TUNG BANG (KE CA BANG NANG CAO) =====';
SELECT t.name AS TenBang, SUM(p.rows) AS SoDong
FROM sys.tables t JOIN sys.partitions p ON p.object_id=t.object_id AND p.index_id IN (0,1)
WHERE t.schema_id=SCHEMA_ID(N'dbo')
GROUP BY t.name ORDER BY t.name;
PRINT N'===== 9. PHAT HIEN NHAM CO SO (KET QUA PHAI RONG) =====';
SELECT N'DangKyKhoaHoc' AS BangLoi, dk.MaDangKy AS MaBanGhi FROM dbo.DangKyKhoaHoc dk
JOIN dbo.HocVien h ON h.MaHocVien=dk.MaHocVien WHERE dk.MaCoSo<>h.MaCoSo
UNION ALL
SELECT N'XepLopMamNon', x.MaXepLop FROM dbo.XepLopMamNon x
JOIN dbo.HocVien h ON h.MaHocVien=x.MaHocVien WHERE x.MaCoSo<>h.MaCoSo
UNION ALL
SELECT N'PhieuThu', p.MaPhieuThu FROM dbo.PhieuThu p
JOIN dbo.HocVien h ON h.MaHocVien=p.MaHocVien WHERE p.MaCoSo<>h.MaCoSo;
PRINT N'===== 10. KIEM TRA DIEU KIEN TOI THIEU =====';
IF (SELECT COUNT(*) FROM dbo.HocVien WHERE GhiChu=N'[TEST VINABACUS 2026]')<12
    THROW 57104,N'Thieu 12 hoc vien/hoc sinh bo sung.',1;
IF (SELECT COUNT(*) FROM dbo.PhieuThu WHERE SoPhieuThu IN ('DEMO-THU-TT-001','DEMO-THU-MN-001'))<>2
    THROW 57105,N'Thieu hai phieu thu mau.',1;
IF (SELECT COUNT(*) FROM dbo.PhieuKho WHERE MaGiaoDichNghiepVu LIKE 'VINABACUS-DEMO-%')<6
    THROW 57106,N'Thieu phieu kho mau.',1;
IF EXISTS(
 SELECT 1 FROM dbo.PhieuThu p JOIN dbo.ChiTietPhieuThu c ON c.MaPhieuThu=p.MaPhieuThu
 WHERE p.SoPhieuThu LIKE 'DEMO-THU-%'
 GROUP BY p.MaPhieuThu,p.TongTien
 HAVING p.TongTien<>SUM(CASE WHEN c.LoaiBienDong='PHAN_BO' THEN c.SoTienPhanBo ELSE -c.SoTienPhanBo END)
) THROW 57107,N'PhieuThu va ChiTietPhieuThu khong khop.',1;
PRINT N'PASS: Du lieu mau co ban cho hai co so da san sang de test MVC.';
GO
