//package dk.itu.minitwit.domain;
//
//import javax.persistence.Entity;
//import javax.persistence.GeneratedValue;
//import javax.persistence.GenerationType;
//import javax.persistence.Id;
//
//@Entity
//public class Message {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.AUTO)
//    private Integer messageId;
//
//    private Integer authorId;
//
//    private Boolean text;
//
//    private Integer pubDate;
//
//    private Integer flagged;
//
//    public Integer getMessageId() {
//        return messageId;
//    }
//
//    public void setMessageId(final Integer messageId) {
//        this.messageId = messageId;
//    }
//
//    public Integer getAuthorId() {
//        return authorId;
//    }
//
//    public void setAuthorId(final Integer authorId) {
//        this.authorId = authorId;
//    }
//
//    public Boolean getText() {
//        return text;
//    }
//
//    public void setText(final Boolean text) {
//        this.text = text;
//    }
//
//    public Integer getPubDate() {
//        return pubDate;
//    }
//
//    public void setPubDate(final Integer pubDate) {
//        this.pubDate = pubDate;
//    }
//
//    public Integer getFlagged() {
//        return flagged;
//    }
//
//    public void setFlagged(final Integer flagged) {
//        this.flagged = flagged;
//    }
//
//}
