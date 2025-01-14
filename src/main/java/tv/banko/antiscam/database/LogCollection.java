package tv.banko.antiscam.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import org.bson.Document;
import tv.banko.antiscam.AntiScam;

import java.util.Optional;

public class LogCollection {

    private final AntiScam antiScam;

    private final String collectionName;
    private final MongoDB mongoDB;

    public LogCollection(AntiScam antiScam, MongoDB mongoDB) {
        this.antiScam = antiScam;

        this.collectionName = "log";
        this.mongoDB = mongoDB;
    }

    public void setChannel(Snowflake guild, Snowflake channel) {
        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        Document document = new Document().append("guildId", guild.asString()).append("channelId", channel.asString());

        if (collection.find(Filters.and(Filters.eq("guildId", guild.asString()))).first() == null) {
            collection.insertOne(document);
            return;
        }

        collection.updateOne(Filters.and(Filters.eq("guildId", guild.asString())),
                new Document("$set", document));
    }

    public void removeChannel(Snowflake guild) {
        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        if (collection.find(Filters.eq("guildId", guild.asString())).first() == null) {
            return;
        }

        collection.deleteMany(Filters.eq("guildId", guild.asString()));
    }

    public void sendMessage(Snowflake guild, EmbedCreateSpec spec) {
        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        Document document = collection.find(Filters.eq("guildId", guild.asString())).first();

        if (document == null) {
            System.out.println("document null");
            return;
        }

        Optional<Channel> optional = antiScam.getGateway().getChannelById(Snowflake.of(
                document.getString("channelId"))).blockOptional();

        if (optional.isEmpty()) {
            removeChannel(guild);
            System.out.println("rm c");
            return;
        }

        GuildMessageChannel channel = (GuildMessageChannel) optional.get();

        channel.createMessage(spec).onErrorStop().block();
    }

    private String getCollectionName() {
        return collectionName;
    }

}
