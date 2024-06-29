package de.feelix.sierra.check.violation;

import de.feelix.sierra.utilities.FormatUtils;
import de.feelix.sierraapi.violation.MitigationStrategy;
import de.feelix.sierraapi.violation.Violation;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class ViolationDocument implements Violation {

    private String             description;
    private MitigationStrategy mitigationStrategy;
    private List<Debug<?>>     debugs;

    @Override
    public String debugInformation() {
        return FormatUtils.chainDebugs(true, debugs);
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public MitigationStrategy mitigationStrategy() {
        return mitigationStrategy;
    }
}
