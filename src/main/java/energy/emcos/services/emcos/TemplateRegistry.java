package energy.emcos.services.emcos;

public interface TemplateRegistry {
	void registerTemplate(String key, String template);
	String getTemplate(String key);
}
