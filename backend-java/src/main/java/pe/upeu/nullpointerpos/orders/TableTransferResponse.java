package pe.upeu.nullpointerpos.orders;

import java.time.OffsetDateTime;

public record TableTransferResponse(Long orderId, String fromTableCode, String toTableCode,
                                    String status, OffsetDateTime transferredAt) { }
