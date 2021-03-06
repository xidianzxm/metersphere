package io.metersphere.notice.service;

import io.metersphere.base.domain.MessageTask;
import io.metersphere.base.domain.MessageTaskExample;
import io.metersphere.base.mapper.MessageTaskMapper;
import io.metersphere.commons.constants.NoticeConstants;
import io.metersphere.commons.user.SessionUser;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.notice.controller.request.MessageRequest;
import io.metersphere.notice.domain.MessageDetail;
import io.metersphere.notice.domain.MessageSettingDetail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class NoticeService {
    @Resource
    private MessageTaskMapper messageTaskMapper;

    public void saveMessageTask(MessageRequest messageRequest) {
        SessionUser user = SessionUtils.getUser();
        String orgId = user.getLastOrganizationId();
        messageRequest.getMessageDetail().forEach(list -> {
            MessageTaskExample example = new MessageTaskExample();
            example.createCriteria().andIdentificationEqualTo(list.getIdentification());
            List<MessageTask> messageTaskLists = messageTaskMapper.selectByExample(example);
            if (messageTaskLists.size() > 0) {
                delMessage(list.getIdentification());
                String identification = UUID.randomUUID().toString();
                list.getUserIds().forEach(m -> {
                    MessageTask message = new MessageTask();
                    message.setId(UUID.randomUUID().toString());
                    message.setEvent(list.getEvent());
                    message.setTaskType(list.getTaskType());
                    message.setUserId(m);
                    message.setType(list.getType());
                    message.setWebhook(list.getWebhook());
                    message.setIdentification(identification);
                    message.setIsSet(list.getIsSet());
                    message.setOrganizationId(orgId);
                    message.setTestId(list.getTestId());
                    messageTaskMapper.insert(message);
                });
            } else {
                String identification = UUID.randomUUID().toString();
                list.getUserIds().forEach(m -> {
                    MessageTask message = new MessageTask();
                    message.setId(UUID.randomUUID().toString());
                    message.setEvent(list.getEvent());
                    message.setTaskType(list.getTaskType());
                    message.setUserId(m);
                    message.setType(list.getType());
                    message.setWebhook(list.getWebhook());
                    message.setIdentification(identification);
                    message.setIsSet(list.getIsSet());
                    message.setOrganizationId(orgId);
                    message.setTestId(list.getTestId());
                    messageTaskMapper.insert(message);
                });
            }

        });
    }

    public List<MessageDetail> searchMessageSchedule(String testId) {
        MessageTaskExample example = new MessageTaskExample();
        example.createCriteria().andTestIdEqualTo(testId);
        List<MessageTask> messageTaskLists = messageTaskMapper.selectByExample(example);
        List<MessageDetail> scheduleMessageTask = new ArrayList<>();
        Map<String, List<MessageTask>> MessageTaskMap = messageTaskLists.stream().collect(Collectors.groupingBy(e -> e.getIdentification()));
        MessageTaskMap.forEach((k, v) -> {
            Set userIds = new HashSet();
            MessageDetail messageDetail = new MessageDetail();
            for (MessageTask m : v) {
                userIds.add(m.getUserId());
                messageDetail.setEvent(m.getEvent());
                messageDetail.setTaskType(m.getTaskType());
                messageDetail.setWebhook(m.getWebhook());
                messageDetail.setIdentification(m.getIdentification());
                messageDetail.setType(m.getType());
                messageDetail.setIsSet(m.getIsSet());
            }
            messageDetail.setUserIds(new ArrayList(userIds));
            scheduleMessageTask.add(messageDetail);
        });
        return scheduleMessageTask;
    }

    public MessageSettingDetail searchMessage() {
        SessionUser user = SessionUtils.getUser();
        String orgId = user.getLastOrganizationId();
        MessageTaskExample messageTaskExample = new MessageTaskExample();
        messageTaskExample.createCriteria().andOrganizationIdEqualTo(orgId);
        List<MessageTask> messageTaskLists = new ArrayList<>();
        MessageSettingDetail messageSettingDetail = new MessageSettingDetail();
        List<MessageDetail> MessageDetailList = new ArrayList<>();
        messageTaskLists = messageTaskMapper.selectByExample(messageTaskExample);
        Map<String, List<MessageTask>> MessageTaskMap = messageTaskLists.stream().collect(Collectors.groupingBy(e -> fetchGroupKey(e)));
        MessageTaskMap.forEach((k, v) -> {
            Set userIds = new HashSet();
            MessageDetail messageDetail = new MessageDetail();
            for (MessageTask m : v) {
                userIds.add(m.getUserId());
                messageDetail.setEvent(m.getEvent());
                messageDetail.setTaskType(m.getTaskType());
                messageDetail.setWebhook(m.getWebhook());
                messageDetail.setIdentification(m.getIdentification());
                messageDetail.setType(m.getType());
                messageDetail.setIsSet(m.getIsSet());
            }
            messageDetail.setUserIds(new ArrayList(userIds));
            MessageDetailList.add(messageDetail);
        });
        List<MessageDetail> jenkinsTask = MessageDetailList.stream().filter(a -> a.getTaskType().equals(NoticeConstants.JENKINS_TASK)).collect(Collectors.toList());
        List<MessageDetail> testCasePlanTask = MessageDetailList.stream().filter(a -> a.getTaskType().equals(NoticeConstants.TEST_PLAN_TASK)).collect(Collectors.toList());
        List<MessageDetail> reviewTask = MessageDetailList.stream().filter(a -> a.getTaskType().equals(NoticeConstants.REVIEW_TASK)).collect(Collectors.toList());
        List<MessageDetail> defectTask = MessageDetailList.stream().filter(a -> a.getTaskType().equals(NoticeConstants.DEFECT_TASK)).collect(Collectors.toList());
        messageSettingDetail.setJenkinsTask(jenkinsTask);
        messageSettingDetail.setTestCasePlanTask(testCasePlanTask);
        messageSettingDetail.setReviewTask(reviewTask);
        messageSettingDetail.setDefectTask(defectTask);
        return messageSettingDetail;
    }

    private static String fetchGroupKey(MessageTask user) {
        return user.getTaskType() + "#" + user.getIdentification();
    }

    public int delMessage(String identification) {
        MessageTaskExample example = new MessageTaskExample();
        example.createCriteria().andIdentificationEqualTo(identification);
        return messageTaskMapper.deleteByExample(example);
    }
}