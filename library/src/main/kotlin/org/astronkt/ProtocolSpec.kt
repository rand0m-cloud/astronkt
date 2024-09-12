package org.astronkt

data class ProtocolMessageSpec(
    val name: String,
    val msgType: UShort,
    val args: List<ProtocolMessageArgumentSpec>
) {
    val isControl: Boolean
        get() = msgType >= 9000U
}

sealed class ProtocolMessageArgumentSpec(open val name: String, open val type: FieldValue.Type) {
    data class Simple(
        override val name: String,
        override val type: FieldValue.Type
    ) : ProtocolMessageArgumentSpec(name, type)

    data object Dynamic : ProtocolMessageArgumentSpec("dynamic", FieldValue.Type.Blob)
}

object ProtocolMessageRepository {
    val protocolMessages: List<ProtocolMessageSpec> = _protocolMessages
    val byMsgType: Map<UShort, ProtocolMessageSpec> = protocolMessages.associateBy { it.msgType }
    val byName: Map<String, ProtocolMessageSpec> = protocolMessages.associateBy { it.name }
}


val _protocolMessages = listOf(
    // Client Messages (1 - 999)
    ProtocolMessageSpec(
        "CLIENT_HELLO",
        1U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("dc_hash", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("version", FieldValue.Type.String)
        )
    ),
    ProtocolMessageSpec(
        "CLIENT_HELLO_RESP",
        2U,
        listOf()
    ),
    ProtocolMessageSpec(
        "CLIENT_DISCONNECT",
        3U,
        listOf()
    ),
    ProtocolMessageSpec(
        "CLIENT_EJECT",
        4U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("error_code", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Simple("reason", FieldValue.Type.String)
        )
    ),
    ProtocolMessageSpec(
        "CLIENT_HEARTBEAT",
        5U,
        listOf()
    ),
    ProtocolMessageSpec(
        "CLIENT_OBJECT_SET_FIELD",
        120U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("field_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "CLIENT_OBJECT_SET_FIELDS",
        121U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("field_count", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "CLIENT_OBJECT_LEAVING",
        132U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "CLIENT_OBJECT_LOCATION",
        140U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "CLIENT_OBJECT_LOCATION",
        140U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "CLIENT_ENTER_OBJECT_REQUIRED",
        142U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("dclass_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic  // Required fields
        )
    ),
    ProtocolMessageSpec(
        "CLIENT_ENTER_OBJECT_REQUIRED_OTHER",
        143U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("dclass_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic  // Required + optional fields
        )
    ),
    ProtocolMessageSpec(
        "CLIENT_DONE_SET_FIELDS",
        160U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "CLIENT_ENTER_OBJECT_REQUIRED_OWNER",
        172U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("dclass_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic  // Required fields
        )
    ),
    ProtocolMessageSpec(
        "CLIENT_ENTER_OBJECT_REQUIRED_OTHER_OWNER",
        173U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("dclass_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic  // Required + optional fields
        )
    ),
    ProtocolMessageSpec(
        "CLIENT_ADD_INTEREST",
        200U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("interest_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "CLIENT_ADD_INTEREST_MULTIPLE",
        201U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("interest_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_count", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "CLIENT_REMOVE_INTEREST",
        203U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("interest_id", FieldValue.Type.UInt16)
        )
    ),
    ProtocolMessageSpec(
        "CLIENT_DONE_INTEREST_RESP",
        204U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("interest_id", FieldValue.Type.UInt16)
        )
    ),

    // Client Agent Messages (1000 - 1999)
    ProtocolMessageSpec(
        "CLIENTAGENT_SET_STATE",
        1000U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("ca_state", FieldValue.Type.UInt16)
        )
    ),
    ProtocolMessageSpec(
        "CLIENTAGENT_SET_CLIENT_ID",
        1001U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("channel", FieldValue.Type.UInt64)
        )
    ),
    ProtocolMessageSpec(
        "CLIENTAGENT_SEND_DATAGRAM",
        1002U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("datagram", FieldValue.Type.Blob)
        )
    ),
    ProtocolMessageSpec(
        "CLIENTAGENT_EJECT",
        1004U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("disconnect_code", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Simple("reason", FieldValue.Type.String)
        )
    ),
    ProtocolMessageSpec(
        "CLIENTAGENT_DROP",
        1005U,
        listOf()
    ),
    ProtocolMessageSpec(
        "CLIENTAGENT_GET_NETWORK_ADDRESS",
        1006U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "CLIENTAGENT_GET_NETWORK_ADDRESS_RESP",
        1007U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("remote_ip", FieldValue.Type.String),
            ProtocolMessageArgumentSpec.Simple("remote_port", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Simple("local_ip", FieldValue.Type.String),
            ProtocolMessageArgumentSpec.Simple("local_port", FieldValue.Type.UInt16)
        )
    ),
    ProtocolMessageSpec(
        "CLIENTAGENT_DECLARE_OBJECT",
        1010U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("dclass_id", FieldValue.Type.UInt16)
        )
    ),
    ProtocolMessageSpec(
        "CLIENTAGENT_UNDECLARE_OBJECT",
        1011U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "CLIENTAGENT_ADD_SESSION_OBJECT",
        1012U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "CLIENTAGENT_REMOVE_SESSION_OBJECT",
        1013U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "CLIENTAGENT_SET_FIELDS_SENDABLE",
        1014U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("field_count", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "CLIENTAGENT_GET_TLVS",
        1015U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "CLIENTAGENT_GET_TLVS_RESP",
        1016U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("tlvs", FieldValue.Type.Blob)
        )
    ),

    // Client Agent Control Messages
    ProtocolMessageSpec(
        "CLIENTAGENT_OPEN_CHANNEL",
        1100U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("channel", FieldValue.Type.UInt64)
        )
    ),
    ProtocolMessageSpec(
        "CLIENTAGENT_CLOSE_CHANNEL",
        1101U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("channel", FieldValue.Type.UInt64)
        )
    ),
    ProtocolMessageSpec(
        "CLIENTAGENT_ADD_POST_REMOVE",
        1110U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("datagram", FieldValue.Type.Blob)
        )
    ),
    ProtocolMessageSpec(
        "CLIENTAGENT_CLEAR_POST_REMOVE",
        1111U,
        listOf()
    ),

    // Client Agent Interest Messages
    ProtocolMessageSpec(
        "CLIENTAGENT_ADD_INTEREST",
        1200U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("interest_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "CLIENTAGENT_ADD_INTEREST_MULTIPLE",
        1201U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("interest_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_count", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "CLIENTAGENT_REMOVE_INTEREST",
        1203U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("interest_id", FieldValue.Type.UInt16)
        )
    ),
    ProtocolMessageSpec(
        "CLIENTAGENT_DONE_INTEREST_RESP",
        1204U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("client_id", FieldValue.Type.UInt64),
            ProtocolMessageArgumentSpec.Simple("interest_id", FieldValue.Type.UInt16)
        )
    ),

    // State Server Messages (2000 - 2999)
    ProtocolMessageSpec(
        "STATESERVER_CREATE_OBJECT_WITH_REQUIRED",
        2000U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("dclass_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_CREATE_OBJECT_WITH_REQUIRED_OTHER",
        2001U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("dclass_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_DELETE_AI_OBJECTS",
        2009U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("ai_channel", FieldValue.Type.UInt64)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_GET_FIELD",
        2010U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("field_id", FieldValue.Type.UInt16)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_GET_FIELD_RESP",
        2011U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("success", FieldValue.Type.Bool),
            ProtocolMessageArgumentSpec.Simple("field_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_GET_FIELDS",
        2012U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("field_count", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_GET_FIELDS_RESP",
        2013U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("success", FieldValue.Type.UInt8),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_GET_ALL",
        2014U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_GET_ALL_RESP",
        2015U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("dclass_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_SET_FIELD",
        2020U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("field_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_SET_FIELDS",
        2021U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("field_count", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_DELETE_FIELD_RAM",
        2030U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("field_id", FieldValue.Type.UInt16)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_DELETE_FIELDS_RAM",
        2031U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("field_count", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_DELETE_RAM",
        2032U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_SET_LOCATION",
        2040U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_CHANGING_LOCATION",
        2041U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("new_parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("new_zone_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("old_parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("old_zone_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_ENTER_LOCATION_WITH_REQUIRED",
        2042U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("dclass_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_ENTER_LOCATION_WITH_REQUIRED_OTHER",
        2043U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("dclass_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_GET_LOCATION",
        2044U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_GET_LOCATION_RESP",
        2045U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_LOCATION_ACK",
        2046U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_SET_AI",
        2050U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("ai_channel", FieldValue.Type.UInt64)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_CHANGING_AI",
        2051U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("new_ai_channel", FieldValue.Type.UInt64),
            ProtocolMessageArgumentSpec.Simple("old_ai_channel", FieldValue.Type.UInt64)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_ENTER_AI_WITH_REQUIRED",
        2052U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("dclass_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_ENTER_AI_WITH_REQUIRED_OTHER",
        2053U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("dclass_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_GET_AI",
        2054U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_GET_AI_RESP",
        2055U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("ai_channel", FieldValue.Type.UInt64)
        )
    ),

    // State Server Object Ownership Messages (2060 - 2063)

    ProtocolMessageSpec(
        "STATESERVER_OBJECT_SET_OWNER",
        2060U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("owner_channel", FieldValue.Type.UInt64)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_CHANGING_OWNER",
        2061U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("new_owner_channel", FieldValue.Type.UInt64),
            ProtocolMessageArgumentSpec.Simple("old_owner_channel", FieldValue.Type.UInt64)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_ENTER_OWNER_WITH_REQUIRED",
        2062U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("dclass_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_ENTER_OWNER_WITH_REQUIRED_OTHER",
        2063U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("dclass_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),

    // State Server Object Interest Messages (2066 - 2067)

    ProtocolMessageSpec(
        "STATESERVER_OBJECT_ENTER_INTEREST_WITH_REQUIRED",
        2066U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("dclass_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_ENTER_INTEREST_WITH_REQUIRED_OTHER",
        2067U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("dclass_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),

    // State Server Parent Object Methods
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_GET_ZONE_OBJECTS",
        2100U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_GET_ZONES_OBJECTS",
        2102U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_count", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_GET_CHILDREN",
        2104U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_GET_ZONE_COUNT",
        2110U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_GET_ZONE_COUNT_RESP",
        2111U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("object_count", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_GET_ZONES_COUNT",
        2112U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_count", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_GET_ZONES_COUNT_RESP",
        2113U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("object_count", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_GET_CHILD_COUNT",
        2114U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_GET_CHILD_COUNT_RESP",
        2115U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("object_count", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_DELETE_ZONE",
        2120U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_DELETE_ZONES",
        2122U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_count", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_OBJECT_DELETE_CHILDREN",
        2124U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_GET_ACTIVE_ZONES",
        2125U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "STATESERVER_GET_ACTIVE_ZONES_RESP",
        2126U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_count", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),

    // Database State Server Messages (2200 - 2299)
    ProtocolMessageSpec(
        "DBSS_OBJECT_ACTIVATE_WITH_DEFAULTS",
        2200U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "DBSS_OBJECT_ACTIVATE_WITH_DEFAULTS_OTHER",
        2201U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("parent_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("zone_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("dclass_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "DBSS_OBJECT_GET_ACTIVATED",
        2207U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "DBSS_OBJECT_GET_ACTIVATED_RESP",
        2208U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("is_activated", FieldValue.Type.Bool)
        )
    ),
    ProtocolMessageSpec(
        "DBSS_OBJECT_DELETE_FIELD_DISK",
        2230U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("field_id", FieldValue.Type.UInt16)
        )
    ),
    ProtocolMessageSpec(
        "DBSS_OBJECT_DELETE_FIELDS_DISK",
        2231U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("field_count", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "DBSS_OBJECT_DELETE_DISK",
        2232U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32)
        )
    ),

    // Database Server Messages (3000 - 3999)
    ProtocolMessageSpec(
        "DBSERVER_CREATE_OBJECT",
        3000U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("dclass_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Simple("field_count", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "DBSERVER_CREATE_OBJECT_RESP",
        3001U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "DBSERVER_OBJECT_GET_FIELD",
        3010U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("field_id", FieldValue.Type.UInt16)
        )
    ),
    ProtocolMessageSpec(
        "DBSERVER_OBJECT_GET_FIELD_RESP",
        3011U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("success", FieldValue.Type.UInt8),
            ProtocolMessageArgumentSpec.Simple("field_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "DBSERVER_OBJECT_GET_FIELDS",
        3012U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("field_count", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "DBSERVER_OBJECT_GET_FIELDS_RESP",
        3013U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("success", FieldValue.Type.UInt8),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "DBSERVER_OBJECT_GET_ALL",
        3014U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32)
        )
    ),
    ProtocolMessageSpec(
        "DBSERVER_OBJECT_GET_ALL_RESP",
        3015U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("success", FieldValue.Type.UInt8),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "DBSERVER_OBJECT_SET_FIELD",
        3020U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("field_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "DBSERVER_OBJECT_SET_FIELDS",
        3021U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("field_count", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "DBSERVER_OBJECT_SET_FIELD_IF_EQUALS",
        3022U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("field_id", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic,
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "DBSERVER_OBJECT_SET_FIELD_IF_EQUALS_RESP",
        3023U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("context", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("success", FieldValue.Type.UInt8),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "DBSERVER_OBJECT_DELETE_FIELD",
        3030U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("field_id", FieldValue.Type.UInt16)
        )
    ),
    ProtocolMessageSpec(
        "DBSERVER_OBJECT_DELETE_FIELDS",
        3031U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32),
            ProtocolMessageArgumentSpec.Simple("field_count", FieldValue.Type.UInt16),
            ProtocolMessageArgumentSpec.Dynamic
        )
    ),
    ProtocolMessageSpec(
        "DBSERVER_OBJECT_DELETE",
        3032U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("do_id", FieldValue.Type.UInt32)
        )
    ),

    // Control Messages (9000 - 9999)

    ProtocolMessageSpec(
        "CONTROL_ADD_CHANNEL",
        9000U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("channel", FieldValue.Type.UInt64)
        )
    ),
    ProtocolMessageSpec(
        "CONTROL_REMOVE_CHANNEL",
        9001U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("channel", FieldValue.Type.UInt64)
        )
    ),
    ProtocolMessageSpec(
        "CONTROL_ADD_RANGE",
        9002U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("low_channel", FieldValue.Type.UInt64),
            ProtocolMessageArgumentSpec.Simple("high_channel", FieldValue.Type.UInt64)
        )
    ),
    ProtocolMessageSpec(
        "CONTROL_REMOVE_RANGE",
        9003U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("low_channel", FieldValue.Type.UInt64),
            ProtocolMessageArgumentSpec.Simple("high_channel", FieldValue.Type.UInt64)
        )
    ),
    ProtocolMessageSpec(
        "CONTROL_ADD_POST_REMOVE",
        9010U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("sender", FieldValue.Type.UInt64),
            ProtocolMessageArgumentSpec.Simple("datagram", FieldValue.Type.Blob)
        )
    ),
    ProtocolMessageSpec(
        "CONTROL_CLEAR_POST_REMOVES",
        9011U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("sender", FieldValue.Type.UInt64)
        )
    ),
    ProtocolMessageSpec(
        "CONTROL_SET_CON_NAME",
        9012U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("name", FieldValue.Type.String)
        )
    ),
    ProtocolMessageSpec(
        "CONTROL_SET_CON_URL",
        9013U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("url", FieldValue.Type.String)
        )
    ),
    ProtocolMessageSpec(
        "CONTROL_LOG_MESSAGE",
        9014U,
        listOf(
            ProtocolMessageArgumentSpec.Simple("message", FieldValue.Type.Blob)
        )
    )
)