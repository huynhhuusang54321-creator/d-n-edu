package vn.vinabacus.doituong.lichhoc;

/**
 * DTO khung cho module Lịch lớp và buổi học. KHONG phai anh xa cot SQL Server.
 * Thanh vien 3 bo sung cac truong thuc te tu schema khi trien khai.
 */
public record ThongTinLichHoc(String ma, String ten, String trangThai) { }
