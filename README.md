# BinaryGlyph Code — Privacy in the Last-Mile Logistics 📦🔒

[Português]
O **BinaryGlyph Code** é um protótipo de segurança cibernética visual focado na proteção de dados (LGPD/GDPR) para etiquetas de e-commerce e logística de última milha. Ele mascara dados sensíveis do comprador (como CPF, telefone e nome completo) transformando strings de texto em glifos lineares dinâmicos.

### 🚀 O Conceito Híbrido (Split Architecture)
* **Geração (Nuvem Privada):** A fórmula de conversão e mascaramento de dados roda em um ambiente fechado de microsserviços (API Cloud/Firebase), protegendo a propriedade intelectual (IP) do algoritmo.
* **Leitura (Edge/Offline):** O aplicativo de entrega utiliza inteligência visual nativa via **Google ML Kit (Vision)** para decodificar e filtrar os dados localmente de forma instantânea, permitindo o funcionamento mesmo sem sinal de internet.

---

[English]
**BinaryGlyph Code** is a visual cybersecurity prototype designed to enforce data privacy (GDPR/LGPD compliance) on e-commerce shipping labels. It masks sensitive buyer information by converting raw text strings into structured, low-density visual glyphs.

### 🧠 Features
* **Role-Based Access Control (RBAC):** The visual interface filters data dynamically. A delivery driver only decodes the street address, the customer sees the full package details, and unauthorized third parties see only generic marketing data.
* **Cloud-Gated Generation:** The core mapping algorithm is hidden behind an enterprise-grade cloud API.

---

## 💼 Business & Licensing (Propriedade Intelectual)

* **Client Application (App):** Provided open-source under the **MIT License** for community evaluation, UI performance testing, and mobile OCR contributions.
* **Enterprise API (Core Gate):** The production-ready backend, cryptographic dynamic table, and token validation stack are **private property**. 

### 📬 Contact for Acquisition / M&A 
If you are an engineering lead, investor, or M&A representative from **Google, Mercado Livre, Amazon, DHL**, or global logistics platforms, contact the inventor for architecture review, IP evaluation, or technology licensing:

* **Inventor:** Waldir Gomes
* **Project Status:** MVP Concept / Open-Core Architecture
* **Contact:** *makeshiftstudios.office@gmail.com*
