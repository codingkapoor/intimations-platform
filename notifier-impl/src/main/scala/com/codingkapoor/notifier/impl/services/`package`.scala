package com.codingkapoor.notifier.impl.services

object `package` {

  object NotificationTemplates {
    final val INTIMATION_CREATION_TITLE_TEMPLATE = s"New intimation from %s"
    final val INTIMATION_UPDATE_TITLE_TEMPLATE = s"Intimation update from %s"
    final val INTIMATION_CANCELLATION_TITLE_TEMPLATE = s"Intimation cancelled by %s"

    final val WFH_WFO_PUSH_NOTIFICATION_TEMPLATE = "WFH first half %s. Check intimation for other planned days."
    final val WFO_WFH_PUSH_NOTIFICATION_TEMPLATE = "WFH second half %s. Check intimation for other planned days."
    final val WFO_LEAVE_PUSH_NOTIFICATION_TEMPLATE = "On leave for second half %s. Check intimation for other planned days."
    final val LEAVE_WFO_PUSH_NOTIFICATION_TEMPLATE = "On leave for first half %s. Check intimation for other planned days."
    final val WFH_LEAVE_PUSH_NOTIFICATION_TEMPLATE = "WFH first half and is on leave for second half %s. Check intimation for other planned days."
    final val LEAVE_WFH_PUSH_NOTIFICATION_TEMPLATE = "On leave for first half and WFH second half %s. Check intimation for other planned days."
    final val WFH_PUSH_NOTIFICATION_TEMPLATE = "WFH %s. Check intimation for other planned days."
    final val LEAVE_PUSH_NOTIFICATION_TEMPLATE = "On leave %s. Check intimation for other planned days."
    final val PLANNED_PUSH_NOTIFICATION_TEMPLATE = "Check intimation for planned days."

    final val WFH_WFO_MAIL_BODY_TEMPLATE = "%s: WFH first half"
    final val WFO_WFH_MAIL_BODY_TEMPLATE = "%s: WFH second half"
    final val WFO_LEAVE_MAIL_BODY_TEMPLATE = "%s: Leave second half"
    final val LEAVE_WFO_MAIL_BODY_TEMPLATE = "%s: Leave first half"
    final val WFH_LEAVE_MAIL_BODY_TEMPLATE = "%s: WFH first half, Leave second half"
    final val LEAVE_WFH_MAIL_BODY_TEMPLATE = "%s: Leave first half, WFH second half"
    final val WFH_MAIL_BODY_TEMPLATE = "%s: WFH"
    final val LEAVE_MAIL_BODY_TEMPLATE = "%s: Leave"
  }

}
