package vn.vinabacus.model;

/**
 * DTO khung cho module Kho, nhập/xuất, kiểm kê. KHONG phai anh xa cot SQL Server.
 * Thanh vien 6 bo sung cac truong thuc te tu schema khi trien khai.
 */
public record ThongTinKho(String ma, String ten, String trangThai) { }
