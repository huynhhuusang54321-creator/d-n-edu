package vn.vinabacus.model;

/**
 * DTO khung cho module Tiền ăn Mầm non. KHONG phai anh xa cot SQL Server.
 * Thanh vien 5 bo sung cac truong thuc te tu schema khi trien khai.
 */
public record ThongTinTienAn(String ma, String ten, String trangThai) { }
