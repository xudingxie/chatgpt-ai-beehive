package cn.beehive.web.service.impl;

import cn.beehive.base.cache.SysParamCache;
import cn.beehive.base.enums.EmailBizTypeEnum;
import cn.beehive.base.resource.email.EmailRegisterLoginConfig;
import cn.beehive.base.resource.email.EmailUtil;
import cn.beehive.web.service.EmailService;
import cn.beehive.web.service.SysEmailSendLogService;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * 邮箱注册类型策略实现类
 *
 * @author CoDeleven
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    /**
     * 邮箱注册模板内容 sysParamKey
     */
    private static final String REGISTER_EMAIL_TEMPLATE_CONTENT_SYS_PARAM_KEY = "email-registerTemplateContent";

    @Resource
    private SysEmailSendLogService emailLogService;

    @Override
    public Boolean sendForVerifyCode(String targetEmail, String verifyCode) {
        // 记录日志
        EmailRegisterLoginConfig emailRegisterLoginConfig = EmailUtil.getRegisterAccountConfig();
        String sendContent = getSendContent(emailRegisterLoginConfig, verifyCode);
        MailAccount mailAccount = EmailUtil.getMailAccount();

        try {
            String sendMsgId = sendMessage(emailRegisterLoginConfig, mailAccount, targetEmail, sendContent);
            emailLogService.createSuccessLogBySysLog(sendMsgId, mailAccount.getFrom(), targetEmail, EmailBizTypeEnum.REGISTER_VERIFY, sendContent);
            return true;
        } catch (Exception e) {
            // 邮件发送失败
            emailLogService.createFailedLogBySysLog("", mailAccount.getFrom(), targetEmail, EmailBizTypeEnum.REGISTER_VERIFY, sendContent, e.getMessage());
            return false;
        }
    }

    /**
     * 发送消息
     *
     * @param emailRegisterLoginConfig 邮箱注册登录配置
     * @param mailAccount              MailAccount
     * @param targetEmail              目标邮件地址
     * @param content                  内容
     * @return 响应
     */
    private String sendMessage(EmailRegisterLoginConfig emailRegisterLoginConfig, MailAccount mailAccount, String targetEmail, String content) {
        return MailUtil.send(mailAccount, targetEmail, emailRegisterLoginConfig.getRegisterTemplateSubject(), content, true);
    }

    /**
     * 获取发送内容
     *
     * @param emailRegisterLoginConfig 邮箱注册登录配置
     * @param verifyCode               验证码
     * @return 发送内容
     */
    private String getSendContent(EmailRegisterLoginConfig emailRegisterLoginConfig, String verifyCode) {
        // 设置模板中需要填充的变量
        Context context = new Context();
        context.setVariable("verificationUrl", emailRegisterLoginConfig.getRegisterVerificationRedirectUrl().concat(verifyCode));
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();

        // 获取邮件模板
        String htmlTemplate = SysParamCache.get(REGISTER_EMAIL_TEMPLATE_CONTENT_SYS_PARAM_KEY);
        // 设置模板模式
        TemplateSpec templateSpec = new TemplateSpec(htmlTemplate, TemplateMode.HTML);
        // 进行渲染
        return templateEngine.process(templateSpec, context);
    }
}
