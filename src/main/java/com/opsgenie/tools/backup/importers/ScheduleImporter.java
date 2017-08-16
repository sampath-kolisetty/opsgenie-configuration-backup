package com.opsgenie.tools.backup.importers;

import com.opsgenie.client.ApiException;
import com.opsgenie.client.api.ScheduleApi;
import com.opsgenie.client.model.*;
import com.opsgenie.tools.backup.BackupUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScheduleImporter extends BaseImporter<Schedule> {

    private static ScheduleApi api = new ScheduleApi();

    public ScheduleImporter(String backupRootDirectory, boolean addEntity, boolean updateEntitiy) {
        super(backupRootDirectory, addEntity, updateEntitiy);
    }

    @Override
    protected BeanStatus checkEntities(Schedule oldEntity, Schedule currentEntity) {
        if (oldEntity.getId().equals(currentEntity.getId())) {
            return isSame(oldEntity, currentEntity) ? BeanStatus.NOT_CHANGED : BeanStatus.MODIFIED;
        }

        if (oldEntity.getName().equals(currentEntity.getName())) {
            oldEntity.setId(currentEntity.getId());
            return isSame(oldEntity, currentEntity) ? BeanStatus.NOT_CHANGED : BeanStatus.MODIFIED;
        }

        return BeanStatus.NOT_EXIST;
    }

    @Override
    protected Schedule getBean() {
        return new Schedule();
    }

    @Override
    protected String getImportDirectoryName() {
        return "schedules";
    }

    @Override
    protected void addBean(Schedule bean) throws ApiException {
        CreateSchedulePayload payload = new CreateSchedulePayload();
        payload.setName(bean.getName());

        if (BackupUtils.checkValidString(bean.getDescription()))
            payload.setDescription(bean.getDescription());

        payload.setTimezone(bean.getTimezone());
        payload.setEnabled(bean.isEnabled());
        payload.setOwnerTeam(bean.getOwnerTeam());
        payload.setRotations(constructCreateScheduleRotationPayloads(bean));

        api.createSchedule(payload);
    }

    @Override
    protected void updateBean(Schedule bean) throws ApiException {
        UpdateSchedulePayload payload = new UpdateSchedulePayload();
        payload.setName(bean.getName());

        if (BackupUtils.checkValidString(bean.getDescription()))
            payload.setDescription(bean.getDescription());

        payload.setTimezone(bean.getTimezone());
        payload.setEnabled(bean.isEnabled());
        payload.setOwnerTeam(bean.getOwnerTeam());
        payload.setRotations(constructCreateScheduleRotationPayloads(bean));

        UpdateScheduleRequest request = new UpdateScheduleRequest();
        request.setIdentifier(bean.getId());
        request.setIdentifierType(UpdateScheduleRequest.IdentifierTypeEnum.ID);
        request.setBody(payload);

        api.updateSchedule(request);
    }

    private List<CreateScheduleRotationPayload> constructCreateScheduleRotationPayloads(Schedule bean) {

        List<CreateScheduleRotationPayload> createScheduleRotationPayloadList = new ArrayList<CreateScheduleRotationPayload>();

        if (bean.getRotations() != null && bean.getRotations().size() > 0) {

            for (ScheduleRotation rotation : bean.getRotations()) {
                if (rotation.getLength() == null || rotation.getLength() < 1) {
                    rotation.setLength(1);
                }

                createScheduleRotationPayloadList.add(new CreateScheduleRotationPayload()
                        .name(rotation.getName())
                        .startDate(rotation.getStartDate())
                        .endDate(rotation.getEndDate())
                        .length(rotation.getLength())
                        .participants(rotation.getParticipants())
                        .timeRestriction(rotation.getTimeRestriction())
                        .type(CreateScheduleRotationPayload.TypeEnum.fromValue(rotation.getType().getValue()))
                );
            }
        }

        return createScheduleRotationPayloadList;
    }

    @Override
    protected List<Schedule> retrieveEntities() throws ApiException {
        return api.listSchedules(Collections.singletonList("rotation")).getData();
    }

    @Override
    protected String getEntityIdentifierName(Schedule entitiy) {
        return "Schedule " + entitiy.getName();
    }

    @Override
    protected boolean isSame(Schedule oldEntity, Schedule currentEntity) {
        if (oldEntity.getRotations() != null && currentEntity.getRotations() != null
                && oldEntity.getRotations().size() == currentEntity.getRotations().size()) {
            for (int i = 0; i < oldEntity.getRotations().size(); i++) {
                oldEntity.getRotations().get(i).setId(null);
                currentEntity.getRotations().get(i).setId(null);
            }
        }
        return super.isSame(oldEntity, currentEntity);
    }
}
