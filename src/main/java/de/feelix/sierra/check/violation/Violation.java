package de.feelix.sierra.check.violation;

import de.feelix.sierraapi.violation.PunishType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class Violation {

    private String         description;
    private double         points;
    private PunishType     punishType;
    private List<Debug<?>> debugs;
}
