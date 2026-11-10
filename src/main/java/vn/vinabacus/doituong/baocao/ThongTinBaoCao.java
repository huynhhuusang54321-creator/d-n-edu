package vn.vinabacus.doituong.baocao;

/**
 * DTO khung cho module Báo cáo, thống kê. KHONG phai anh xa cot SQL Server.
 * Thanh vien 6 bo sung cac truong thuc te tu schema khi trien khai.
 */
public record ThongTinBaoCao(String ma, String ten, String trangThai) { }
