import nodemailer from 'nodemailer';

import { smtpConfig, mailSenderConfig, mailList } from '../config/mailConfig';

let transport = nodemailer.createTransport({
    host: smtpConfig.host,
    port: smtpConfig.port,
    secure: false,
    auth: {
        user: smtpConfig.auth.user,
        pass: smtpConfig.auth.pass
    }
});

export const sendMailNotification = (notification) => {
    let mailOptions = {
        from: mailSenderConfig.from,
        to: mailList.mails.join(","),
        subject: notification.title,
        text: notification.content,
    };
    transport.sendMail(mailOptions, (error, info) => {
        if (error) {
            return console.log(error);
        }
        console.log('Message sent: %s', info.messageId);
    });
}