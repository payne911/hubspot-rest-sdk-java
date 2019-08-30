package com.smartling.connector.hubspot.sdk.rest.api;

import com.smartling.connector.hubspot.sdk.common.ListWrapper;
import com.smartling.connector.hubspot.sdk.email.EmailDetail;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;

import java.util.Map;

public interface EmailsEntityApi
{
    @RequestLine("GET /marketing-emails/v1/emails?offset={offset}&limit={limit}&order={order}&property=" + EmailDetail.FIELDS)
    ListWrapper<EmailDetail> list(@Param("offset") int offset, @Param("limit") int limit,
                                    @Param("order") String orderBy, @QueryMap Map<String, Object> queryMap);

    @RequestLine("GET /marketing-emails/v1/emails/{email_id}?property=" + EmailDetail.FIELDS)
    EmailDetail getDetail(@Param("email_id") String emailId);
}
