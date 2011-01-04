package akka.persistence.redis;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

import akka.actor.ActorRef;
import akka.actor.Actors;
import akka.actor.Actor;
import akka.actor.UntypedActorFactory;

public class RedisStorageTests {

	private ActorRef chat = null;

    @Before public void initialise() {
		RedisStorageBackend.flushDB();
		chat = Actors.actorOf(new UntypedActorFactory() {
			public Actor create() {
				return new RedisChatStorage();
			}
		});
		chat.start();
    }

    @Test public void doChat() {
		chat.sendOneWay(new ChatMessage("debasish", "hi there"));
		ChatLog cl = (ChatLog)chat.sendRequestReply(new GetChatLog("debasish"));
		assertEquals(1, cl.getLog().size());
		chat.sendOneWay(new ChatMessage("debasish", "hi again"));
		cl = (ChatLog)chat.sendRequestReply(new GetChatLog("debasish"));
		assertEquals(2, cl.getLog().size());
    }
}

