package com.myrealtrip.commonweb.aop.allowedip

import com.myrealtrip.common.codes.response.ErrorCode
import com.myrealtrip.common.exceptions.BizRuntimeException

class UnauthorizedIpException(
    clientIp: String,
) : BizRuntimeException(ErrorCode.UNAUTHORIZED_IP, clientIp)