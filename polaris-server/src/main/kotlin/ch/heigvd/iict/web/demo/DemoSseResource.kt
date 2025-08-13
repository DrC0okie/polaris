package ch.heigvd.iict.web.demo

import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.sse.Sse
import jakarta.ws.rs.sse.SseBroadcaster
import jakarta.ws.rs.sse.SseEventSink
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@ApplicationScoped
@Path("/demo")
class DemoSseResource {

    @Context
    lateinit var sse: Sse

    @Inject
    lateinit var json: Json

    private lateinit var broadcaster: SseBroadcaster

    @PostConstruct
    fun init() {
        broadcaster = sse.newBroadcaster()
    }

    @GET
    @Path("/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    fun events(@Context sink: SseEventSink) {
        broadcaster.register(sink)
    }

    fun publish(event: DemoEvent) {
        val jsonString = json.encodeToString(event)

        val name = event::class.simpleName ?: "Event"
        val out = sse.newEventBuilder()
            .name(name)
            .mediaType(MediaType.APPLICATION_JSON_TYPE)
            .data(jsonString)
            .build()
        broadcaster.broadcast(out)
    }
}