/*
 * GIAI DOAN 4 - DU THAO DU LIEU, CHUA DUOC THUC THI.
 * Da duoc chu du an chon: lop 1 = 20, lop 2 = 15; lop 3/4 giu NULL.
 * Khong tao/sua bang, khong xoa du lieu, khong doi xep lop hay lich hoc.
 * Can duyet lai tren ban sao va doi chieu live truoc khi chay tren CSDL that.
 */
SET XACT_ABORT ON;
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;

IF DB_NAME() <> N'VINABACUS'
    THROW 58100, N'Script chi danh cho CSDL VINABACUS.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    IF NOT EXISTS (SELECT 1 FROM dbo.CoSo WITH (HOLDLOCK)
                   WHERE MaCoSo=2 AND LoaiCoSo='TRUNG_TAM' AND TrangThai=1)
        THROW 58101, N'MaCoSo 2 khong phai Trung tam dang hoat dong.', 1;

    DECLARE @MucTieu TABLE (
        MaLop INT NOT NULL PRIMARY KEY,
        MaKhoaHocDuKien INT NOT NULL,
        SiSoToiDaMoi INT NOT NULL
    );
    INSERT INTO @MucTieu (MaLop, MaKhoaHocDuKien, SiSoToiDaMoi)
    VALUES (1, 1, 20), (2, 1, 15);

    IF EXISTS (SELECT 1 FROM @MucTieu WHERE SiSoToiDaMoi<=0)
        THROW 58102, N'Si so moi phai lon hon 0.', 1;

    -- Khong ghi de neu lop/khoa da doi hoac gioi han da duoc nhap tu giao dien.
    IF EXISTS (
        SELECT 1 FROM @MucTieu m
        LEFT JOIN dbo.LopTrungTam l WITH (UPDLOCK, HOLDLOCK)
          ON l.MaLop=m.MaLop AND l.MaCoSo=2
        WHERE l.MaLop IS NULL OR l.MaKhoaHoc<>m.MaKhoaHocDuKien
           OR l.SiSoToiDa IS NOT NULL
    )
        THROW 58103, N'Du lieu lop 1/2 da doi; dung de doi chieu lai.', 1;

    IF (SELECT COUNT(*) FROM dbo.LopTrungTam WITH (UPDLOCK, HOLDLOCK)
        WHERE MaCoSo=2 AND ((MaLop=3 AND MaKhoaHoc=3)
                         OR (MaLop=4 AND MaKhoaHoc=1)))<>2
        THROW 58104, N'Lop 3/4 da doi hoac khong ton tai; dung de doi chieu lai.', 1;

    IF EXISTS (
        SELECT 1 FROM dbo.LopTrungTam l WITH (UPDLOCK, HOLDLOCK)
        WHERE l.MaCoSo=2 AND l.MaLop IN (3,4) AND l.SiSoToiDa IS NOT NULL
    )
        THROW 58107, N'Lop 3/4 da co gioi han; dung de khong ghi de quyet dinh moi.', 1;

    -- Gioi han rieng phai du cho hoc vien da giu cho va khong vuot phong
    -- da gan cho lich con hieu luc / buoi du kien. Lop chua co phong van
    -- duoc dat gioi han rieng, nhung ung dung chua nhan them hoc vien.
    IF EXISTS (
        SELECT 1
        FROM @MucTieu m
        JOIN dbo.LopTrungTam l WITH (UPDLOCK, HOLDLOCK)
          ON l.MaLop=m.MaLop AND l.MaCoSo=2
        OUTER APPLY (
            SELECT COUNT(DISTINCT d.MaHocVien) AS DaGiuCho
            FROM dbo.XepLopTrungTam x WITH (HOLDLOCK)
            JOIN dbo.DangKyKhoaHoc d
              ON d.MaDangKy=x.MaDangKy AND d.MaKhoaHoc=x.MaKhoaHoc
             AND d.MaCoSo=x.MaCoSo
            WHERE x.MaLop=l.MaLop AND x.MaCoSo=l.MaCoSo
              AND x.TrangThai='HIEU_LUC'
              AND (x.DenNgay IS NULL OR x.DenNgay>=CONVERT(date,GETDATE()))
        ) g
        OUTER APPLY (
            SELECT MIN(r.SucChua) AS SucChuaNhoNhat
            FROM (
                SELECT p.SucChua FROM dbo.LichHocLop lh WITH (HOLDLOCK)
                JOIN dbo.PhongHoc p WITH (HOLDLOCK)
                  ON p.MaPhong=lh.MaPhong AND p.MaCoSo=l.MaCoSo
                WHERE lh.MaLop=l.MaLop AND lh.TrangThai='HIEU_LUC'
                  AND (lh.DenNgay IS NULL OR lh.DenNgay>=CONVERT(date,GETDATE()))
                UNION ALL
                SELECT p.SucChua FROM dbo.BuoiHoc b WITH (HOLDLOCK)
                JOIN dbo.PhongHoc p WITH (HOLDLOCK)
                  ON p.MaPhong=b.MaPhong AND p.MaCoSo=l.MaCoSo
                WHERE b.MaLop=l.MaLop AND b.TrangThai='DU_KIEN'
                  AND b.NgayHoc>=CONVERT(date,GETDATE())
            ) r
        ) phong
        WHERE m.SiSoToiDaMoi<g.DaGiuCho
           OR m.SiSoToiDaMoi>phong.SucChuaNhoNhat
    )
        THROW 58105, N'Gioi han moi nho hon so da giu cho hoac lon hon suc chua phong.', 1;

    UPDATE l SET SiSoToiDa=m.SiSoToiDaMoi
    FROM dbo.LopTrungTam l
    JOIN @MucTieu m ON m.MaLop=l.MaLop
    WHERE l.MaCoSo=2 AND l.MaKhoaHoc=m.MaKhoaHocDuKien
      AND l.SiSoToiDa IS NULL;

    IF @@ROWCOUNT<>2
        THROW 58106, N'So lop duoc cap nhat khong bang 2; da rollback.', 1;

    SELECT MaLop, MaKhoaHoc, SiSoToiDa
    FROM dbo.LopTrungTam
    WHERE MaCoSo=2 AND MaLop IN (1,2,3,4)
    ORDER BY MaLop;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
