package com.smartling.connector.hubspot.sdk.rest.api;

import com.smartling.connector.hubspot.sdk.DeletePageInfo;
import com.smartling.connector.hubspot.sdk.PageDetail;
import com.smartling.connector.hubspot.sdk.PageDetails;
import feign.Param;
import feign.RequestLine;

public interface PagesEntityApi
{
    @RequestLine("GET /content/api/v2/pages?access_token={access_token}&limit={limit}&offset={offset}")
    PageDetails pages(@Param("limit") int limit, @Param("offset") int offset, @Param("access_token") String accessToken);

    @RequestLine("GET /content/api/v2/pages?access_token={access_token}&tms_id={tms_id}")
    PageDetails findByTmsId(@Param("tms_id") String tmsId, @Param("access_token") String accessToken);

    @RequestLine("DELETE /content/api/v2/pages/{page_id}?access_token={access_token}")
    DeletePageInfo delete(@Param("page_id") long pageId, @Param("access_token") String accessToken);

    @RequestLine("GET /content/api/v2/pages/{page_id}?access_token={access_token}")
    PageDetail pageDetail(@Param("page_id") long pageId, @Param("access_token") String accessToken);

    @RequestLine("POST /content/api/v2/pages/{page_id}/clone?access_token={access_token}")
    PageDetail clone(@Param("page_id") long pageId, @Param("access_token") String accessToken);
}
