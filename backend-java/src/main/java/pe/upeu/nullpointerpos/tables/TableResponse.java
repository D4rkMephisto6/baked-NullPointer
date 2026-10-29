package pe.upeu.nullpointerpos.tables;

public record TableResponse(Long tableId, String tableCode, String areaName, Integer capacity,
                            String status, boolean active) { }
