package vn.vinabacus.model;

/**
 * DTO khung cho module Nhân viên, tài khoản, cơ sở. KHONG phai anh xa cot SQL Server.
 * Thanh vien 6 bo sung cac truong thuc te tu schema khi trien khai.
 */
public record ThongTinHeThong(String ma, String ten, String trangThai) { }
