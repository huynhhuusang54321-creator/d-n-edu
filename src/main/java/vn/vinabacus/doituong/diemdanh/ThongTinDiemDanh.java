package vn.vinabacus.doituong.diemdanh;

/**
 * DTO khung cho module Điểm danh. KHONG phai anh xa cot SQL Server.
 * Thanh vien 4 bo sung cac truong thuc te tu schema khi trien khai.
 */
public record ThongTinDiemDanh(String ma, String ten, String trangThai) { }
