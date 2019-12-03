let pushNotificationHandler = require('./pushNotificationsHandler');

let pushNotification = {};

const formatDate = function(date) {

    let day = `${date.getDate()}`.padStart(2,0);
    let month = `${date.getMonth() + 1}`.padStart(2,0);
    let year = date.getFullYear();
  
    return year + '-' + month + '-' + day;
}
  
const getPushNotificationMessage = function(message){
    pushNotification = {};
    let today = new Date();
    let tomorrow = new Date(today);
    let hasMoreThanOneRequest = message.requests.length === 1 ? false : message.requests.length > 1 ? true : '';
    tomorrow.setDate(tomorrow.getDate() + 1)
    today = formatDate(today);
    tomorrow = formatDate(tomorrow);
    if(message.requests){
        let item = message.requests[0];
        let dayText = (item.date === today) ? ' today' :  (item.date === tomorrow) ? ' tomorrow' : '';
        pushNotification.title = getPushNotificationTitle(message, item, dayText, hasMoreThanOneRequest);
        pushNotification.content = message.reason;
        return;
    }
    pushNotificationHandler.handlePushTokens(pushNotification);
}

const getPushNotificationTitle = function(message, item, day, hasMoreThanOneRequest){
    let appendFirstValText = (item.firstHalf === 'Leave') ? 'on' : '';
    let appendSecondValText = (item.secondHalf === 'Leave') ? 'on' : '';
    let appendText = hasMoreThanOneRequest ? ' and has planned leaves/WFH for subsequent days' : '';
    let messageText = '';
    if(day){
        (item.firstHalf === item.secondHalf) ?  messageText = message.id + ' is ' + appendFirstValText + item.firstHalf  + day + appendText :
        (item.firstHalf === 'WFO') ? messageText = message.id + ' is ' + appendSecondValText + item.secondHalf + 'in second half ' + day + appendText :
        (item.secondHalf === 'WFO') ? messageText = message.id + ' is ' + appendFirstValText + item.firstHalf + 'in first half' + day + appendText :
        messageText = message.id + ' is ' + appendFirstValText + item.firstHalf + 'in first half and' + appendSecondValText + item.secondHalf + 'in second half' + day + appendText;
        return messageText;
    }else{
        return message.id + ' has planned WFH/Leaves for subsequent days';
    }
}

const getPushNotificationMessageForCancelledIntimation = function(message){
    pushNotification = {};
    pushNotification.title = 'Cancelled Intimation';
    pushNotification.content = message.reason;
    pushNotificationHandler.handlePushTokens(pushNotification);
}

module.exports = {
    getPushNotificationMessage: getPushNotificationMessage,
    getPushNotificationMessageForCancelledIntimation: getPushNotificationMessageForCancelledIntimation
};
