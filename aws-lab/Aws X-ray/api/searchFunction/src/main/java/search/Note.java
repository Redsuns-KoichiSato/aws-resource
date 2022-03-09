package search;

public class Note {
    private String UserId;
    private String NoteId;
    private String Note;

    public String getNote() {
        return Note;
    }

    public void setNote(String Note) {
        this.Note = Note;
    }

    public String getNoteId() {
        return NoteId;
    }

    public void setNoteId(String NoteId) {
        this.NoteId = NoteId;
    }

    public String getUserId() {
        return UserId;
    }

    public void setUserId(String UserId) {
        this.UserId = UserId;
    }
}
