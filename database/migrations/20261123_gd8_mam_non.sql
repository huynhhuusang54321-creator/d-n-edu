-- Giai đoạn 8: bổ sung khối, sĩ số tối đa và chính sách tiền ăn theo tháng.
-- Chỉ chuẩn bị để duyệt; không chạy trên VINABACUS khi chưa được phê duyệt riêng.
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRAN;
    IF OBJECT_ID(N'dbo.LopMamNon',N'U') IS NULL OR OBJECT_ID(N'dbo.CoSo',N'U') IS NULL
        THROW 51080, N'Thiếu bảng gốc LopMamNon hoặc CoSo.', 1;

    IF COL_LENGTH('dbo.LopMamNon','Khoi') IS NULL
    BEGIN
        ALTER TABLE dbo.LopMamNon ADD Khoi VARCHAR(5) NULL;
        EXEC sys.sp_addextendedproperty @name=N'VINABACUS_GD8', @value=N'20261123',
            @level0type=N'SCHEMA',@level0name=N'dbo',@level1type=N'TABLE',@level1name=N'LopMamNon',
            @level2type=N'COLUMN',@level2name=N'Khoi';
    END;
    IF COL_LENGTH('dbo.LopMamNon','SiSoToiDa') IS NULL
    BEGIN
        ALTER TABLE dbo.LopMamNon ADD SiSoToiDa INT NULL;
        EXEC sys.sp_addextendedproperty @name=N'VINABACUS_GD8', @value=N'20261123',
            @level0type=N'SCHEMA',@level0name=N'dbo',@level1type=N'TABLE',@level1name=N'LopMamNon',
            @level2type=N'COLUMN',@level2name=N'SiSoToiDa';
    END;
    IF OBJECT_ID(N'dbo.CK_LopMamNon_Khoi_GD8',N'C') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.LopMamNon WITH CHECK ADD CONSTRAINT CK_LopMamNon_Khoi_GD8 CHECK (Khoi IS NULL OR Khoi IN (''MAM'',''CHOI'',''LA''))';
    IF OBJECT_ID(N'dbo.CK_LopMamNon_SiSoToiDa_GD8',N'C') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.LopMamNon WITH CHECK ADD CONSTRAINT CK_LopMamNon_SiSoToiDa_GD8 CHECK (SiSoToiDa IS NULL OR SiSoToiDa>0)';

    IF OBJECT_ID(N'dbo.ChinhSachTienAnMamNon',N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.ChinhSachTienAnMamNon (
            MaChinhSach INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ChinhSachTienAnMamNon PRIMARY KEY,
            MaCoSo INT NOT NULL,
            ThangBatDau DATE NOT NULL,
            TinhCoMat BIT NOT NULL,
            TinhVangCoPhep BIT NOT NULL,
            TinhVangKhongPhep BIT NOT NULL,
            GhiChu NVARCHAR(500) NULL,
            CONSTRAINT FK_ChinhSachTienAnMamNon_CoSo FOREIGN KEY (MaCoSo) REFERENCES dbo.CoSo(MaCoSo),
            CONSTRAINT UQ_ChinhSachTienAnMamNon_Ky UNIQUE (MaCoSo,ThangBatDau),
            CONSTRAINT CK_ChinhSachTienAnMamNon_DauThang CHECK (DAY(ThangBatDau)=1)
        );
        EXEC sys.sp_addextendedproperty @name=N'VINABACUS_GD8', @value=N'20261123',
            @level0type=N'SCHEMA',@level0name=N'dbo',@level1type=N'TABLE',@level1name=N'ChinhSachTienAnMamNon';
    END;
    IF OBJECT_ID(N'dbo.NgayHoatDongMamNon',N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.NgayHoatDongMamNon (
            MaNgayHoatDong INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_NgayHoatDongMamNon PRIMARY KEY,
            MaNamHoc INT NOT NULL,
            MaCoSo INT NOT NULL,
            Ngay DATE NOT NULL,
            CoHoc BIT NOT NULL,
            CoAn BIT NOT NULL,
            GhiChu NVARCHAR(300) NULL,
            CONSTRAINT FK_NgayHoatDongMamNon_NamHoc FOREIGN KEY (MaNamHoc,MaCoSo)
                REFERENCES dbo.NamHoc(MaNamHoc,MaCoSo),
            CONSTRAINT UQ_NgayHoatDongMamNon_Ngay UNIQUE (MaCoSo,Ngay),
            CONSTRAINT CK_NgayHoatDongMamNon_AnHoc CHECK (CoAn=0 OR CoHoc=1)
        );
        EXEC sys.sp_addextendedproperty @name=N'VINABACUS_GD8', @value=N'20261123',
            @level0type=N'SCHEMA',@level0name=N'dbo',@level1type=N'TABLE',@level1name=N'NgayHoatDongMamNon';
    END;
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK;
    THROW;
END CATCH;

-- Cố ý không gán khối/sĩ số cho lớp cũ và không tạo dòng chính sách mẫu.
-- Không tạo lịch ngày mặc định: từng ngày trong tháng phải được nhà trường xác nhận.
-- Người quản lý xác nhận từng lớp; mã Java dùng quy tắc tiền ăn tạm thời đã duyệt
-- cho tới khi có chính sách cấu hình theo ThangBatDau.
