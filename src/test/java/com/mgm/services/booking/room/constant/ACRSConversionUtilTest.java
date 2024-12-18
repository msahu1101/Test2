package com.mgm.services.booking.room.constant;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ACRSConversionUtilTest {

    @Test
    public void testRateCodeGeneration() {

        final String ratePlanCode = "RP1";
        final String propertyCode = "MV021";
        final String groupCode = "GRPCD-v-GRP1-d-PROP-v-MV021";
        final String ratePlanId = ACRSConversionUtil.createRatePlanCodeGuid(ratePlanCode, propertyCode);

        Assert.assertEquals(false, ACRSConversionUtil.isAcrsRatePlanGuid(groupCode));
        Assert.assertEquals(true, ACRSConversionUtil.isAcrsRatePlanGuid(ratePlanId));
        Assert.assertEquals(ratePlanCode, ACRSConversionUtil.getRatePlanCode(ratePlanId));
        Assert.assertEquals(propertyCode, ACRSConversionUtil.getPropertyCode(ratePlanId));
    }

    @Test
    public void testGroupCodeGeneration() {

        final String groupCode = "GRP1";
        final String propertyCode = "MV021";
        final String ratePlanCode = "RPCD-v-RP1-d-PROP-v-MV021";
        final String groupCodeId = ACRSConversionUtil.createGroupCodeGuid(groupCode, propertyCode);

        Assert.assertEquals(false, ACRSConversionUtil.isAcrsGroupCodeGuid(ratePlanCode));
        Assert.assertEquals(true, ACRSConversionUtil.isAcrsGroupCodeGuid(groupCodeId));
        Assert.assertEquals(groupCode, ACRSConversionUtil.getGroupCode(groupCodeId));
        Assert.assertEquals(propertyCode, ACRSConversionUtil.getPropertyCode(groupCodeId));

    }

    @Test
    public void testRoomCodeGeneration() {

        final String roomCode = "DLUX";
        final String propertyCode = "MV021";

        final String roomCodeId = ACRSConversionUtil.createRoomCodeGuid(roomCode, propertyCode);
        Assert.assertEquals(true, ACRSConversionUtil.isAcrsRoomCodeGuid(roomCodeId));
        Assert.assertEquals(roomCode, ACRSConversionUtil.getRoomCode(roomCodeId));
        Assert.assertEquals(propertyCode, ACRSConversionUtil.getPropertyCode(roomCodeId));

    }

    @Test
    public void testComponentCodeGeneration() {

        final String componentCode = "EARLYCHECKIN";
        final String propertyCode = "MV021";
        final String componentType = "Component";
        final String nrpRatePlanCode = "RP001";
        final String ratePlanCodeId = "RPCD-v-RP1-d-PROP-v-MV021";

        final String componentId = ACRSConversionUtil.createComponentGuid(componentCode, nrpRatePlanCode, componentType, propertyCode);
        Assert.assertEquals(false, ACRSConversionUtil.isAcrsComponentCodeGuid(ratePlanCodeId));
        Assert.assertEquals(true, ACRSConversionUtil.isAcrsComponentCodeGuid(componentId));
        Assert.assertEquals(componentCode, ACRSConversionUtil.getComponentCode(componentId));
        Assert.assertEquals(nrpRatePlanCode, ACRSConversionUtil.getComponentNRPlanCode(componentId));
        Assert.assertEquals(componentType, ACRSConversionUtil.getComponentType(componentId));
        Assert.assertEquals(propertyCode, ACRSConversionUtil.getPropertyCode(componentId));

    }

}
