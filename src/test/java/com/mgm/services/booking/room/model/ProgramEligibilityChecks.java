package com.mgm.services.booking.room.model;

import lombok.Data;

public @Data class ProgramEligibilityChecks {

    private ProgramEligibility nonMemberProgramTransientUser;
    private ProgramEligibility nonMemberProgramMlifeUser;
    private ProgramEligibility transientProgramTransientUser;
    private ProgramEligibility transientProgramMlifeUser;
    private ProgramEligibility casinoProgramTransientUser;
    private ProgramEligibility casinoProgramMlifeUser;
    private ProgramEligibility casinoSapphireProgramSapphireMlifeUser;
    private ProgramEligibility casinoGoldProgramSapphireMlifeUser;
    private ProgramEligibility patronProgramTransientUser;
    private ProgramEligibility patronProgramNonListedMlifeUser;
    private ProgramEligibility patronProgramListedMlifeUser;
    private ProgramEligibility perpetualProgramTransientUser;
    private ProgramEligibility perpetualProgramDiffSegmentMlifeUser;
    private ProgramEligibility perpetualProgramSameSegmentMlifeUser;
    private ProgramEligibility myvegasProgramTransientUser;
    private ProgramEligibility myvegasProgramMlifeUser;
    private ProgramEligibility casinoProgramJwbFlow;
    private ProgramEligibility hdePackageFlow;
}
