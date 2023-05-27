package cn.beehive.cell.bing.handler;

import cn.beehive.base.domain.entity.RoomBingDO;
import cn.beehive.cell.bing.domain.request.RoomBingMsgSendRequest;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author hncboy
 * @date 2023/5/26
 * Bing API 请求处理
 */
public class BingApiRequestHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String CHAT_REQUEST_JSON_STR = ResourceUtil.readUtf8Str("bing/send.json");

    /**
     * 生成随机 IP
     *
     * @return IP
     */
    public static String generateRandomIp() {
        int a = RandomUtil.randomInt(104, 108);
        int b = RandomUtil.randomInt(256);
        int c = RandomUtil.randomInt(256);
        return "13." + a + "." + b + "." + c;
    }

    /**
     * 构建 Bing API 请求参数
     *
     * @param roomBingDO             房间信息
     * @param roomBingMsgSendRequest 用户请求
     * @return 请求参数
     */
    public static String buildChatRequest(RoomBingDO roomBingDO, RoomBingMsgSendRequest roomBingMsgSendRequest) {
        // 先替换字符串
        String sendStr = CHAT_REQUEST_JSON_STR
                .replace("$conversationId", roomBingDO.getConversationId())
                .replace("$conversationSignature", roomBingDO.getConversationSignature())
                .replace("$clientId", roomBingDO.getClientId())
                .replace("$prompt", roomBingMsgSendRequest.getContent());
//        return sendStr;
        try {
            // 转换到 JsonNode
            JsonNode jsonNode = OBJECT_MAPPER.readTree(sendStr);
            // 获取 "arguments" 数组的第一个元素，转换为 ObjectNode
            ObjectNode objectNode = (ObjectNode) jsonNode.get("arguments").get(0);
            // 替换 boolean，第一条消息为 true，其他为 false；如果第一条消息为 false 会报错，如果其他为 true，则会一直重复第一条的回答
            objectNode.put("isStartOfSession", roomBingDO.getNumUserMessagesInConversation() == 0);
            return OBJECT_MAPPER.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
