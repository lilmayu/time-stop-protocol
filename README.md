# Time Stop Protocol
A network protocol used in Sakuya Bridge: Time Stop.

I have decided to make this protocol standalone, so I can easily use it in other projects.

## Features
- Packet response manager
- Translators
  - Basic Translator
  - Segment Translator
- Encryption (+ Encryption translator)
- Other stuff

### Packet response manager
This is a feature that allows you to send a packet and wait for a response based on the data type.

```java
TimeStopServer server = /* ... */;
TimeStopClient client = /* ... */;

server.getListenerManager().registerListener(new TimeStopListener<Packets.ProtocolVersionExchange>(Packets.ProtocolVersionExchange.class, 0) {
    @Override
    public void process(@NonNull Context context, Packets.@NonNull ProtocolVersionExchange message) {
        context.getConnection().sendTCP(new Packets.ProtocolVersionExchange(1));
    }
});

client.sendTCPWithResponse(packet, Packets.ProtocolVersionExchange.class, response -> {
    assertEquals(1, response.getProtocolVersion());
});
```

### Translators

#### Basic Translator
Translators are used to translate the packet before sending or after receiving.

You may see TimeStopPacketTranslator, TimeStopPacketSegmentTranslator and TimeStopPacketEncryptionTranslator translators for in practice example.

```java
// UUID randomizer before sending

TimeStopServer server = /* ... */;

server.getTranslatorManager().registerTranslator(new TimeStopTranslator(0) {
    @Override
    public Object translate(Context context, Object object) {
        // Ignore incoming packets
        if (context.getWay() == Context.Way.INBOUND) {
            return object;
        }
        
        // If the object is UUID, then we will randomize it
        if (object instanceof UUID) {
            return UUID.randomUUID();
        }

        return object;
    }
});
```

#### Segment Translator
Segment translator can be used to translate TimeStopPackets that are too big for the network (e.g., `TimeStopPacketTranslator#BUFFER_SIZE`)

Simply, install a SegmentTranslator and you are good to go.

```java
TimeStopServer server = /* ... */;
TimeStopClient client = /* ... */;

// Required
server.getTranslatorManager().registerTranslator(new TimeStopPacketTranslator());
client.getTranslatorManager().registerTranslator(new TimeStopPacketTranslator());

// Maximum object size (here NetworkConstants.OBJECT_BUFFER_SIZE)
server.getTranslatorManager().registerTranslator(new TimeStopSegmentTranslator(NetworkConstants.OBJECT_BUFFER_SIZE));
client.getTranslatorManager().registerTranslator(new TimeStopSegmentTranslator(NetworkConstants.OBJECT_BUFFER_SIZE));
```

### Encryption
Encryption translator can be used to encrypt the packets before sending or after receiving.

```java

// TODO: Why the hell TimeStopPacketEncryptionTranslator is using symmetric keys for encryption? Should use asymmetric ones with some automatic exchange action...

```

## Examples
See tests for more examples.

### Creating server
```java
EndpointConfig endpointConfig = /* ... */;
TimeStopServer server = new TimeStopServer(endpointConfig);

server.bind(port);
server.start();

// ...

server.close();
```

### Creating client
```java
EndpointConfig endpointConfig = /* ... */;
TimeStopClient client = new TimeStopClient(endpointConfig);

client.start();
client.connect("localhost", port);

// ...

client.close();
```

### Registering a Time Stop Listener
```java
TimeStopServer server = /* ... */;
TimeStopClient client = /* ... */;

UUID uuidToSend = UUID.randomUUID();

// 100 is priority
server.getListenerManager().registerListener(new TimeStopListener<UUID>(UUID.class, 100) {
    @Override
    public void process(@NonNull Context context, @NonNull UUID message) {
        assertEquals(uuidToSend, message);
    }
});

client.sendTCP(uuidToSend);
```