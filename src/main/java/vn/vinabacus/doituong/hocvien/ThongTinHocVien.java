package vn.vinabacus.doituong.hocvien;

/**
 * DTO khung cho module Học viên. KHONG phai anh xa cot SQL Server.
 * Thanh vien 2 bo sung cac truong thuc te tu schema khi trien khai.
 */
public record ThongTinHocVien(String ma, String ten, String trangThai) { }
