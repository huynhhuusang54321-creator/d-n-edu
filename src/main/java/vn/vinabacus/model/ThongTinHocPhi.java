package vn.vinabacus.model;

/**
 * DTO khung cho module Học phí, giảm giá, công nợ. KHONG phai anh xa cot SQL Server.
 * Thanh vien 5 bo sung cac truong thuc te tu schema khi trien khai.
 */
public record ThongTinHocPhi(String ma, String ten, String trangThai) { }
