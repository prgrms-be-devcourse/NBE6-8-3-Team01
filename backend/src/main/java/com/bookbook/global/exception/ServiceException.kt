package com.bookbook.global.exception

import com.bookbook.global.rsdata.RsData

class ServiceException(private val resultCode: String, private val msg: String) : RuntimeException(
    resultCode + " : " + msg
) {
    // 단순 메시지만으로 생성하는 생성자 추가
    constructor(msg: String) : this("400", msg)

    val rsData: RsData<Void>
        get() = RsData<Void>(resultCode, msg, null)
}