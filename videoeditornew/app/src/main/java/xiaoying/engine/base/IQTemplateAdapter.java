package xiaoying.engine.base;

/* JADX INFO: loaded from: classes18.dex */
public interface IQTemplateAdapter {
    String getTemplateExternalFile(long templateID, int subTemplateID, int fileID);

    String getTemplateFile(long templateID);

    long getTemplateID(String templateFile);
}
