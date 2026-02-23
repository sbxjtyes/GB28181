package com.gb28181.sipserver.service;

import com.gb28181.sipserver.util.SipUtils;
import org.springframework.stereotype.Service;

/**
 * SIP消息模板服务
 * 
 * 提供各种SIP消息的模板，包括：
 * - 注册响应模板
 * - 心跳响应模板
 * - 推流请求模板
 * - 断流请求模板
 * 
 * @author GB28181 Team
 * @version 1.0.0
 */
@Service
public class SipMessageTemplate {

    private static final String CRLF = "\r\n";

    /**
     * 401未授权响应模板
     */
    private static final String TEMPLATE_401_UNAUTHORIZED = 
            "SIP/2.0 401 Unauthorized" + CRLF +
            "CSeq: 1 REGISTER" + CRLF +
            "Call-ID: {Call-ID}" + CRLF +
            "From: {From}" + CRLF +
            "To: {To}" + CRLF +
            "Via: {Via}" + CRLF +
            "WWW-Authenticate: Digest realm=\"{realm}\",nonce=\"{nonce}\"" + CRLF +
            "Content-Length: 0" + CRLF +
            CRLF;

    /**
     * 200 OK注册成功响应模板
     */
    private static final String TEMPLATE_200_OK_REGISTER = 
            "SIP/2.0 200 OK" + CRLF +
            "CSeq: {CSeq}" + CRLF +
            "Call-ID: {Call-ID}" + CRLF +
            "From: {From}" + CRLF +
            "To: {To}" + CRLF +
            "Via: {Via}" + CRLF +
            "Expires: {Expires}" + CRLF +
            "Date: {Date}" + CRLF +
            "Content-Length: 0" + CRLF +
            CRLF;

    /**
     * 心跳保活200 OK响应模板
     */
    private static final String TEMPLATE_200_OK_KEEPALIVE = 
            "SIP/2.0 200 OK" + CRLF +
            "CSeq: {CSeq}" + CRLF +
            "Call-ID: {Call-ID}" + CRLF +
            "From: {From}" + CRLF +
            "To: {To}" + CRLF +
            "Via: {Via}" + CRLF +
            "Content-Length: 0" + CRLF +
            CRLF;

    /**
     * INVITE推流请求模板
     */
    private static final String TEMPLATE_INVITE = 
            "INVITE sip:{deviceId}@{deviceLocalIp}:{deviceLocalPort};transport=udp SIP/2.0" + CRLF +
            "Call-ID: {Call-ID}" + CRLF +
            "CSeq: 1 INVITE" + CRLF +
            "From: <sip:{serverId}@{serverIp}:{serverPort}>;tag=live" + CRLF +
            "To: \"{deviceId}\" <sip:{deviceId}@{deviceLocalIp}:{deviceLocalPort}>" + CRLF +
            "Via: SIP/2.0/UDP {serverIp}:{serverPort};branch=branchlive" + CRLF +
            "Max-Forwards: 70" + CRLF +
            "Content-Type: Application/sdp" + CRLF +
            "Contact: <sip:{serverId}@{serverIp}:{serverPort}>" + CRLF +
            "Supported: 100re1" + CRLF +
            "Subject: {deviceId}:010000{ssrc},{serverId}:0" + CRLF +
            "User-Agent: GB28181-SIP-Server" + CRLF +
            "Content-Length: {Content-Length}" + CRLF +
            CRLF;

    /**
     * SDP媒体描述模板（TCP）
     */
    private static final String TEMPLATE_SDP_TCP =
            "v=0" + CRLF +
            "o={deviceId} 0 0 IN IP4 {mediaServerIp}" + CRLF +
            "s=Play" + CRLF +
            "c=IN IP4 {mediaServerIp}" + CRLF +
            "t=0 0" + CRLF +
            "m=video {mediaServerPort} TCP/RTP/AVP 96 98 97" + CRLF +
            "a=recvonly" + CRLF +
            "a=rtpmap:96 PS/90000" + CRLF +
            "a=rtpmap:98 H264/90000" + CRLF +
            "a=rtpmap:97 MPEG4/90000" + CRLF +
            "y=010000{ssrc}" + CRLF +
            "f=" + CRLF;

    /**
     * SDP媒体描述模板（UDP）
     */
    private static final String TEMPLATE_SDP_UDP =
            "v=0" + CRLF +
            "o={deviceId} 0 0 IN IP4 {mediaServerIp}" + CRLF +
            "s=Play" + CRLF +
            "c=IN IP4 {mediaServerIp}" + CRLF +
            "t=0 0" + CRLF +
            "m=video {mediaServerPort} RTP/AVP 96 98 97" + CRLF +
            "a=recvonly" + CRLF +
            "a=rtpmap:96 PS/90000" + CRLF +
            "a=rtpmap:98 H264/90000" + CRLF +
            "a=rtpmap:97 MPEG4/90000" + CRLF +
            "y=010000{ssrc}" + CRLF +
            "f=" + CRLF;

    /**
     * ACK确认消息模板
     */
    private static final String TEMPLATE_ACK = 
            "ACK sip:{deviceId}@{deviceLocalIp}:{deviceLocalPort} SIP/2.0" + CRLF +
            "Call-ID: {Call-ID}" + CRLF +
            "CSeq: 1 ACK" + CRLF +
            "Via: SIP/2.0/UDP {serverIp}:{serverPort};branch={branchId}" + CRLF +
            "From: {From}" + CRLF +
            "To: {To}" + CRLF +
            "Max-Forwards: 70" + CRLF +
            "Content-Length: 0" + CRLF +
            CRLF;

    /**
     * BYE断流请求模板
     */
    private static final String TEMPLATE_BYE = 
            "BYE sip:{deviceId}@{deviceLocalIp}:{deviceLocalPort};transport=udp SIP/2.0" + CRLF +
            "Call-ID: {Call-ID}" + CRLF +
            "CSeq: 6 BYE" + CRLF +
            "From: {From}" + CRLF +
            "To: {To}" + CRLF +
            "Via: SIP/2.0/UDP {serverIp}:{serverPort};branch=branchbye" + CRLF +
            "Contact: <sip:{serverId}@{serverIp}:{serverPort}>" + CRLF +
            "Max-Forwards: 70" + CRLF +
            "Content-Length: 0" + CRLF +
            CRLF;

    /**
     * 生成401未授权响应
     */
    public String build401Unauthorized(String callId, String from, String to, String via, 
                                     String realm, String nonce) {
        return TEMPLATE_401_UNAUTHORIZED
                .replace("{Call-ID}", callId)
                .replace("{From}", from)
                .replace("{To}", to)
                .replace("{Via}", via)
                .replace("{realm}", realm)
                .replace("{nonce}", nonce);
    }

    /**
     * 生成200 OK注册成功响应
     */
    public String build200OkRegister(String cseq, String callId, String from, String to, 
                                   String via, String expires) {
        return TEMPLATE_200_OK_REGISTER
                .replace("{CSeq}", cseq)
                .replace("{Call-ID}", callId)
                .replace("{From}", from)
                .replace("{To}", to)
                .replace("{Via}", via)
                .replace("{Expires}", expires)
                .replace("{Date}", SipUtils.getGMT());
    }

    /**
     * 生成心跳保活200 OK响应
     */
    public String build200OkKeepalive(String cseq, String callId, String from, String to, String via) {
        return TEMPLATE_200_OK_KEEPALIVE
                .replace("{CSeq}", cseq)
                .replace("{Call-ID}", callId)
                .replace("{From}", from)
                .replace("{To}", to)
                .replace("{Via}", via);
    }

    /**
     * 生成INVITE推流请求
     */
    public String buildInviteRequest(String deviceId, String deviceLocalIp, String deviceLocalPort,
                                   String callId, String serverId, String serverIp, String serverPort,
                                   String ssrc, String mediaServerIp, String mediaServerPort, 
                                   boolean useTcp) {
        String sdpContent = useTcp ? TEMPLATE_SDP_TCP : TEMPLATE_SDP_UDP;
        sdpContent = sdpContent
                .replace("{deviceId}", deviceId)
                .replace("{mediaServerIp}", mediaServerIp)
                .replace("{mediaServerPort}", mediaServerPort)
                .replace("{ssrc}", ssrc);

        String inviteMessage = TEMPLATE_INVITE
                .replace("{deviceId}", deviceId)
                .replace("{deviceLocalIp}", deviceLocalIp)
                .replace("{deviceLocalPort}", deviceLocalPort)
                .replace("{Call-ID}", callId)
                .replace("{serverId}", serverId)
                .replace("{serverIp}", serverIp)
                .replace("{serverPort}", serverPort)
                .replace("{ssrc}", ssrc)
                .replace("{Content-Length}", String.valueOf(sdpContent.getBytes().length));

        return inviteMessage + sdpContent;
    }

    /**
     * 生成ACK确认消息
     */
    public String buildAckMessage(String deviceId, String deviceLocalIp, String deviceLocalPort,
                                String callId, String serverIp, String serverPort, String from, String to) {
        return TEMPLATE_ACK
                .replace("{deviceId}", deviceId)
                .replace("{deviceLocalIp}", deviceLocalIp)
                .replace("{deviceLocalPort}", deviceLocalPort)
                .replace("{Call-ID}", callId)
                .replace("{serverIp}", serverIp)
                .replace("{serverPort}", serverPort)
                .replace("{branchId}", SipUtils.getBranchId())
                .replace("{From}", from)
                .replace("{To}", to);
    }

    /**
     * 生成BYE断流请求
     */
    public String buildByeRequest(String deviceId, String deviceLocalIp, String deviceLocalPort,
                                String callId, String from, String to, String serverId, 
                                String serverIp, String serverPort) {
        return TEMPLATE_BYE
                .replace("{deviceId}", deviceId)
                .replace("{deviceLocalIp}", deviceLocalIp)
                .replace("{deviceLocalPort}", deviceLocalPort)
                .replace("{Call-ID}", callId)
                .replace("{From}", from)
                .replace("{To}", to)
                .replace("{serverId}", serverId)
                .replace("{serverIp}", serverIp)
                .replace("{serverPort}", serverPort);
    }
}
