package vn.vinabacus.model;

/**
 * DTO khung cho module Lớp học và phân công. KHONG phai anh xa cot SQL Server.
 * Thanh vien 3 bo sung cac truong thuc te tu schema khi trien khai.
 */
public record ThongTinLopHoc(String ma, String ten, String trangThai) { }
