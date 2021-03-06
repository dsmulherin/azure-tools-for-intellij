package com.microsoft.intellij.runner.webapp.webappconfig

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.util.idea.getLogger
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.utils.AzureModel
import com.microsoft.intellij.runner.AzureRunConfigurationBase
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppMvpModel
import com.microsoft.intellij.runner.webapp.model.DotNetWebAppSettingModel
import com.microsoft.intellij.runner.webapp.webappconfig.validator.ProjectValidator.validateProject
import com.microsoft.intellij.runner.webapp.webappconfig.validator.SqlDatabaseValidator
import com.microsoft.intellij.runner.webapp.webappconfig.validator.SqlDatabaseValidator.validateDatabaseConnection
import com.microsoft.intellij.runner.webapp.webappconfig.validator.WebAppValidator.validateWebApp

class RiderWebAppConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
        AzureRunConfigurationBase<DotNetWebAppSettingModel>(project, factory, name) {

    companion object {
        private val LOG = getLogger<RiderWebAppConfiguration>()
    }

    private val myModel = DotNetWebAppSettingModel()

    init {
        myModel.webAppModel.publishableProject = project.solution.publishableProjectsModel.publishableProjects.values
                .sortedWith(compareBy({ it.isWeb }, { it.projectName })).firstOrNull()
    }

    override fun getSubscriptionId(): String {
        return myModel.webAppModel.subscription?.subscriptionId() ?: ""
    }

    override fun getTargetPath(): String {
        return myModel.webAppModel.publishableProject?.projectFilePath ?: ""
    }

    override fun getTargetName(): String {
        return myModel.webAppModel.publishableProject?.projectName ?: ""
    }

    override fun getModel(): DotNetWebAppSettingModel {
        return myModel
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return RiderWebAppSettingEditor(project, this)
    }

    @Throws(ExecutionException::class)
    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment): RunProfileState? {
        return RiderWebAppRunState(project, myModel)
    }

    override fun validate() { }

    /**
     * Validate the configuration to run
     *
     * @throws [RuntimeConfigurationError] when configuration miss expected fields
     */
    @Throws(RuntimeConfigurationError::class)
    override fun checkConfiguration() {
        validateAzureAccountIsSignedIn()

        validateProject(myModel.webAppModel.publishableProject)
        validateWebApp(myModel.webAppModel)
        validateDatabaseConnection(myModel.databaseModel)

        if (myModel.databaseModel.isDatabaseConnectionEnabled) {
            checkConnectionStringNameExistence(myModel.databaseModel.connectionStringName, myModel.webAppModel.webAppId)
        }
    }

    /**
     * Check whether user is signed in to Azure account
     *
     * @throws [ConfigurationException] in case validation is failed
     */
    @Throws(RuntimeConfigurationError::class)
    private fun validateAzureAccountIsSignedIn() {
        try {
            if (!AuthMethodManager.getInstance().isSignedIn) {
                val message = UiConstants.SIGN_IN_REQUIRED
                LOG.error(message)
                throw RuntimeConfigurationError(message)
            }
        } catch (e: Throwable) {
            LOG.error(e)
            throw RuntimeConfigurationError(UiConstants.SIGN_IN_REQUIRED)
        }
    }

    /**
     * Check Connection String name existence for a web app
     *
     * @param name connection string name
     * @param webAppId a web app to check
     * @throws [RuntimeConfigurationError] when connection string with a specified name already configured for a web app
     */
    @Throws(RuntimeConfigurationError::class)
    private fun checkConnectionStringNameExistence(name: String, webAppId: String) {
        SqlDatabaseValidator.checkValueIsSet(name, UiConstants.CONNECTION_STRING_NAME_NOT_DEFINED)
        val resourceGroupToWebAppMap = AzureModel.getInstance().resourceGroupToWebAppMap
        if (resourceGroupToWebAppMap == null) {
            LOG.error("AzureModel.resourceGroupToWebAppMap map is NULL")
            return
        }
        val webApp = resourceGroupToWebAppMap
                .flatMap { it.value }
                .firstOrNull { it.id() == webAppId } ?: return

        if (AzureDotNetWebAppMvpModel.checkConnectionStringNameExists(webApp, name))
            throw RuntimeConfigurationError(String.format(UiConstants.CONNECTION_STRING_NAME_ALREADY_EXISTS, name))
    }
}
