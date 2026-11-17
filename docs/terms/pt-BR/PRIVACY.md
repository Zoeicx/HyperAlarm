Esta política explica como o HyperAlarm (doravante, «o módulo») trata suas informações. A conclusão, antes de tudo: o módulo não coleta nem envia nenhum dado pessoal; seus registros e configurações permanecem neste dispositivo. Os detalhes seguem abaixo.

## 0. Versão e data de vigência

- 0.1. Versão da política: 1.0
- 0.2. Data de vigência: 15 de novembro de 2026
- 0.3. Esta política é distribuída com cada compilação.

## 1. Informações armazenadas neste dispositivo

- 1.1. Registros de scripts: horário, código de saída, saída padrão e erro padrão de cada execução de script. Armazenados no banco de dados do módulo e em seu diretório de armazenamento externo.
- 1.2. Registros do aplicativo: o registro de execução do próprio módulo, usado para diagnosticar problemas.
- 1.3. Registros de falhas: resumos curtos de exceções usados pelo modo de segurança, mantidos no diretório privado do módulo.
- 1.4. Configurações: idioma, nível de registro, filtros de lista e similares, mantidos no arquivo de configuração do módulo.
- 1.5. Senha da tela de bloqueio: é gravada em um arquivo somente se você a informar voluntariamente nas configurações; o arquivo tem permissões 600 e só pode ser lido por root e por este módulo; serve para desbloquear a tela antes da execução de uma tarefa silenciosa.
- 1.6. Nada disso sai do seu dispositivo, a menos que você mesmo exporte, compartilhe ou publique em um relato.

## 2. Informações que não coletamos

- 2.1. Não coletamos identificadores do dispositivo (IMEI, Android ID, número de série etc.).
- 2.2. Não coletamos dados de localização.
- 2.3. Não coletamos contatos, mensagens ou registros de chamadas.
- 2.4. Não coletamos o conteúdo dos seus scripts.
- 2.5. O módulo não inclui SDK de análise, de relatório de falhas ou de publicidade.

## 3. Os dois acessos à rede

- 3.1. A página «Sobre» solicita interfaces públicas do GitHub para ler o avatar do autor e a lista de colaboradores. As solicitações são enviadas apenas para api.github.com e avatars.githubusercontent.com, incluem o User-Agent padrão exigido pelo GitHub e não contêm nenhum identificador do dispositivo ou do usuário. Por isso, o GitHub verá seu endereço IP.
- 3.2. O assistente de configuração inicial e a página de configurações baixam do GitHub o texto do contrato de usuário e desta política, para que os documentos possam ser atualizados independentemente das atualizações de versão. Essas solicitações também contêm apenas cabeçalhos padrão. Se o download falhar, será exibida a cópia incluída no pacote de instalação ou um aviso de falha.
- 3.3. Além disso, o módulo não faz nenhuma solicitação de rede.

## 4. Compartilhamento com terceiros

- 4.1. Não compartilhamos informações com terceiros porque não coletamos nenhuma.
- 4.2. As solicitações ao GitHub acima são uma interação direta sua com o GitHub e estão sujeitas à política de privacidade do GitHub.
- 4.3. Quando você exporta e compartilha registros, o destinatário é escolhido por você; isso não constitui um compartilhamento por nossa parte.

## 5. Retenção e exclusão

- 5.1. Os registros são reduzidos automaticamente a um limite máximo fixo, excluindo primeiro os mais antigos.
- 5.2. Você pode limpar os registros no aplicativo ou excluir diretamente os dados do módulo.
- 5.3. Desinstalar o módulo exclui todos os dados de seus diretórios privados.
- 5.4. As cópias dos registros no armazenamento externo precisam ser excluídas manualmente por você antes da desinstalação.

## 6. Seus direitos

- 6.1. Você pode consultar, exportar e apagar a qualquer momento todas as informações armazenadas pelo módulo.
- 6.2. Você pode recusar qualquer permissão; o recurso correspondente ficará indisponível.
- 6.3. Você pode apagar a senha da tela de bloqueio nas configurações; o conteúdo do arquivo será esvaziado.

## 7. Menores de idade

- 7.1. O módulo destina-se a usuários com certo conhecimento do sistema. Menores de idade devem usá-lo sob a orientação de um responsável.
- 7.2. Como envolve root e execução de scripts, não recomendamos que menores o utilizem de forma independente.

## 8. Alterações nesta política

- 8.1. Esta política pode ser revisada por ocasião de uma atualização de versão; o texto revisado é distribuído com a nova compilação e publicado no repositório.
- 8.2. Se uma revisão introduzir novas formas de tratamento de informações, isso será indicado expressamente nas notas da versão.

## 9. Contato

- 9.1. Para questões de privacidade, envie um relato pelo repositório do projeto.
- 9.2. O código-fonte do módulo é público e auditável; você pode verificar por conta própria as afirmações acima.
